"""
End-to-end tests for Nitte Resolve: drives the deployed application over HTTP
exactly like a browser (real JSF form posts with ViewState, multipart uploads)
and checks the results in MySQL.

Requirements: the WAR deployed, MySQL loaded with schema.sql + sample-data.sql,
the mysql command-line client on the PATH (root without password, as in
db.properties), Python 3.9+ with no extra packages.

    python3 tests/e2e/test_e2e.py
    CC_BASE_URL=http://localhost:8086/campus-complaint python3 tests/e2e/test_e2e.py

The run creates one complaint (and its history) for the demo student. Reset the
demo data afterwards with reset-database.sql + schema.sql + sample-data.sql.
"""
import sys, os, re, zlib, struct, subprocess
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from jsf import Browser, login, BASE

results = []
def check(name, cond, detail=""):
    results.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else "  -> " + str(detail)[:300]))

def sql(q):
    return subprocess.run(["mysql", "-uroot", "-N", "-B", "campus_complaint_db", "-e", q], capture_output=True, text=True).stdout.strip()

def png(w=4, h=3):
    raw = b"".join(b"\x00" + b"\xff\x80\x00" * w for _ in range(h))
    chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")

# ---------------------------------------------------------------- 18 DB connectivity
check("18 database reachable with sample data", sql("SELECT COUNT(*) FROM complaints") >= "15")

# ---------------------------------------------------------------- 16 unauthorized access
anon = Browser()
for page in ["/student-dashboard.xhtml", "/admin-dashboard.xhtml", "/staff-dashboard.xhtml", "/notifications.xhtml", "/reports.xhtml"]:
    anon.get(page)
    check("16 anonymous %s redirected to login" % page, anon.url.endswith("/login.xhtml"), anon.url)
anon.get("/attachment?id=1"); check("16 anonymous attachment redirected to login", anon.url.endswith("/login.xhtml"), anon.url)
for page in ["/", "/index.xhtml", "/login.xhtml", "/register.xhtml"]:
    anon.get(page); check("public page %s is 200" % page, anon.status == 200, anon.status)

# ---------------------------------------------------------------- 1 registration
email = "test.student.%d@nitte.edu.in" % os.getpid()
b = Browser(); b.get("/register.xhtml")
b.form("registerForm").set("name", "Test Student").set("email", email).set("password", "short").set("confirm", "short").submit("Create account")
check("15 register rejects weak password", any("8 characters" in m for m in b.messages()), b.messages())
b.get("/register.xhtml")
b.form("registerForm").set("name", "Test Student").set("email", "ananya.rao@nitte.edu.in").set("password", "Passw0rd!").set("confirm", "Passw0rd!").submit("Create account")
check("15 register rejects duplicate e-mail", any("already exists" in m for m in b.messages()), b.messages())
b.get("/register.xhtml")
b.form("registerForm").set("name", "Test Student").set("email", email).set("password", "Passw0rd1").set("confirm", "Passw0rd2").submit("Create account")
check("15 register rejects mismatched passwords", any("do not match" in m for m in b.messages()), b.messages())
b.get("/register.xhtml")
b.form("registerForm").set("name", "Test Student").set("email", email).set("phone", "+91 90000 00000").set("department", "B.E. Civil").set("password", "Passw0rd1").set("confirm", "Passw0rd1").submit("Create account")
check("1 registration succeeds and redirects to login", b.url.endswith("/login.xhtml") and any("account is ready" in m for m in b.messages()), (b.url, b.messages()))
row = sql("SELECT role, password_hash FROM users WHERE email='%s'" % email)
check("1 registered account is STUDENT with PBKDF2 hash", row.startswith("STUDENT\tpbkdf2_sha256$210000$"), row)
b = login(email, "Passw0rd1")
check("1 new student can log in", b.url.endswith("/student-dashboard.xhtml"), b.url)

bad = login("ananya.rao@nitte.edu.in", "wrong-password")
check("login with wrong password refused", bad.url.endswith("/login.xhtml") and any("Invalid" in m for m in bad.messages()), bad.messages())

# ---------------------------------------------------------------- 2/3 admin and staff login
admin = login("admin@nitte.edu.in", "Admin@123")
check("2 administrator login lands on admin dashboard", admin.url.endswith("/admin-dashboard.xhtml") and "Status distribution" in admin.body, admin.url)
suresh = login("suresh.poojary@nitte.edu.in", "Staff@123")
check("3 staff login lands on staff dashboard", suresh.url.endswith("/staff-dashboard.xhtml") and "Open work orders" in suresh.body, suresh.url)
ananya = login("ananya.rao@nitte.edu.in", "Student@123")
check("student login lands on student dashboard", ananya.url.endswith("/student-dashboard.xhtml") and "Hello, Ananya" in ananya.text(), ananya.url)

# role checks
ananya.get("/admin-dashboard.xhtml")
check("16 student cannot open admin dashboard", ananya.url.endswith("/student-dashboard.xhtml") and "not allowed" in ananya.body, ananya.url)
ananya.get("/staff-complaint-details.xhtml?id=5")
check("16 student cannot open staff page", ananya.url.endswith("/student-dashboard.xhtml"), ananya.url)
suresh.get("/manage-users.xhtml")
check("10 staff cannot open admin pages", suresh.url.endswith("/staff-dashboard.xhtml"), suresh.url)
admin.get("/submit-complaint.xhtml")
check("16 admin cannot open the student complaint form", admin.url.endswith("/admin-dashboard.xhtml"), admin.url)

# ---------------------------------------------------------------- 15 invalid submissions
ananya.get("/submit-complaint.xhtml")
ananya.form("complaintForm").submit("Submit complaint")
errs = ananya.field_errors()
check("15 empty complaint shows field errors", len(errs) >= 5 and "Please give the complaint a short title." in errs, errs)
before = sql("SELECT COUNT(*) FROM complaints")
ananya.get("/submit-complaint.xhtml")
f = ananya.form("complaintForm")
f.set("title", "Hi").set("category", "3").set("description", "too short").set("block", "Main Block").set("floor", "Ground floor").set("location", "R").set("priority", "LOW")
f.submit("Submit complaint")
check("15 too-short fields rejected", "between 5 and 150" in ananya.body and "between 15 and 4000" in ananya.body, ananya.field_errors())
ananya.get("/submit-complaint.xhtml")
f = ananya.form("complaintForm")
f.set("title", "Tampered priority test").set("category", "3").set("description", "Trying to set URGENT priority from the form").set("block", "Main Block").set("floor", "Ground floor").set("location", "Room 1").raw("complaintForm:priority", "URGENT")
f.submit("Submit complaint")
check("15 reporter cannot submit URGENT priority", sql("SELECT COUNT(*) FROM complaints") == before, ananya.messages() + ananya.field_errors())

def complaint_form(title):
    ananya.get("/submit-complaint.xhtml")
    return ananya.form("complaintForm").set("title", title).set("category", "2").set("description", "The power socket next to the lab door sparks when a plug is inserted.") \
        .set("block", "Laboratory Complex").set("floor", "1st floor").set("location", "Electronics Lab 1").set("priority", "HIGH")

# ---------------------------------------------------------------- 17 upload validation
complaint_form("Sparking socket fake image").file("image", "photo.png", b"this is not an image at all", "image/png").submit("Submit complaint")
check("17 fake PNG content rejected", any("not a valid JPG or PNG" in m for m in ananya.messages()), ananya.messages())
complaint_form("Sparking socket exe").file("image", "virus.exe", b"MZ\x90\x00", "application/octet-stream").submit("Submit complaint")
check("17 non-image extension rejected", any("Only JPG, JPEG and PNG" in m for m in ananya.messages()), ananya.messages())
complaint_form("Sparking socket big").file("image", "big.png", b"\x89PNG\r\n\x1a\n" + b"0" * (6 * 1024 * 1024), "image/png").submit("Submit complaint")
check("17 oversized image rejected", any("too large" in m for m in ananya.messages()), ananya.messages())
complaint_form("Sparking socket mismatch").file("image", "photo.jpg", png(), "image/jpeg").submit("Submit complaint")
check("17 extension/content mismatch rejected", any("does not match" in m for m in ananya.messages()), ananya.messages())
check("17 no complaint stored for invalid uploads", sql("SELECT COUNT(*) FROM complaints") == before)

# ---------------------------------------------------------------- 4/5 valid submission
complaint_form("Sparking power socket in Electronics Lab 1").file("image", "../../etc/socket.png", png(), "image/png").submit("Submit complaint")
msgs = ananya.messages()
m = re.search(r"Complaint (NIT-\d{4}-\d{5}) has been submitted", " ".join(msgs))
check("4 valid complaint submitted and redirected to My complaints", m and ananya.url.split("?")[0].endswith("/my-complaints.xhtml"), (ananya.url, msgs))
code = m.group(1) if m else ""
cid = sql("SELECT id FROM complaints WHERE complaint_code='%s'" % code)
row = sql("SELECT status, priority, attachment_path FROM complaints WHERE id=%s" % (cid or 0))
check("5 complaint code is NIT-<year>-<5 digits> matching the id", code == "NIT-2026-%05d" % int(cid or 0), (code, cid))
check("5 complaint codes are unique", sql("SELECT COUNT(*) - COUNT(DISTINCT complaint_code) FROM complaints") == "0")
check("4 stored as SUBMITTED / HIGH with a random UUID file name", re.match(r"SUBMITTED\tHIGH\t[0-9a-f-]{36}\.png$", row), row)
check("9 first history row created (NULL -> SUBMITTED)", sql("SELECT CONCAT(IFNULL(previous_status,'NULL'),'>',new_status) FROM complaint_status_history WHERE complaint_id=%s" % cid) == "NULL>SUBMITTED")
check("13 reporter and admin notified of new complaint", sql("SELECT COUNT(*) FROM notifications WHERE complaint_id=%s" % cid) == "2")
check("6 new complaint listed and highlighted", code in ananya.body and "is-highlight" in ananya.body)
uploads = os.path.expanduser("~/campus-complaint-uploads")
check("17 file stored outside the webapp under a generated name", os.path.isfile(os.path.join(uploads, row.split("\t")[2])), uploads)

# attachment access
ananya.get("/attachment?id=%s" % cid)
check("17 owner can view own attachment", ananya.status == 200 and ananya.headers.get("Content-Type") == "image/png" and ananya.raw.startswith(b"\x89PNG"), ananya.status)
rahul = login("rahul.shetty@nitte.edu.in", "Student@123")
rahul.get("/attachment?id=%s" % cid)
check("11/17 other student gets 404 for the attachment", rahul.status == 404, rahul.status)
suresh.get("/attachment?id=%s" % cid)
check("17 unassigned staff gets 404 for the attachment", suresh.status == 404, suresh.status)
admin.get("/attachment?id=%s" % cid)
check("17 admin can view the attachment", admin.status == 200, admin.status)

# ---------------------------------------------------------------- 11 privacy
rahul.get("/complaint-details.xhtml?id=%s" % cid)
check("11 other student cannot open the complaint", "Complaint not found" in rahul.body and "Sparking power socket" not in rahul.body)
rahul.get("/my-complaints.xhtml")
check("11 other student's list does not include it", code not in rahul.body)
ananya.get("/complaint-details.xhtml?id=%s" % cid)
check("6 owner sees details, rail and history", "Sparking power socket" in ananya.body and 'class="rail"' in ananya.body and "Complaint submitted by Ananya Rao" in ananya.body)

# ---------------------------------------------------------------- 14 search
ananya.get("/my-complaints.xhtml")
ananya.form("filterForm").set("keyword", code).submit("Search")
rows = re.findall(r'<span class="code">(NIT-[^<]+)</span>', ananya.body)
check("14 student search by ID finds exactly that complaint", rows == [code], rows)
ananya.get("/my-complaints.xhtml")
ananya.form("filterForm").set("status", "CLOSED").submit("Search")
rows = re.findall(r'<span class="code">(NIT-[^<]+)</span>', ananya.body)
check("14 student status filter", rows and all(sql("SELECT status FROM complaints WHERE complaint_code='%s'" % r) == "CLOSED" for r in rows) and all(sql("SELECT user_id FROM complaints WHERE complaint_code='%s'" % r) == "2" for r in rows), rows)
ananya.get("/my-complaints.xhtml")
ananya.form("filterForm").set("keyword", "%").submit("Search")
check("14 LIKE wildcard in search is escaped", "No complaints match" in ananya.body)

# ---------------------------------------------------------------- 7/8/9 admin review and assignment
admin.get("/complaint-review.xhtml?id=%s" % cid)
check("admin review page shows complaint and reporter", "Sparking power socket" in admin.body and "ananya.rao@nitte.edu.in" in admin.body)
admin.form("priorityForm").set("priority", "URGENT").submit("Update priority")
check("8 admin changes priority to URGENT", sql("SELECT priority FROM complaints WHERE id=%s" % cid) == "URGENT" and any("priority has been updated" in m for m in admin.messages()), admin.messages())
admin.form("assignForm").set("staff", "6").set("assignNote", "Isolate the circuit first").submit("Assign complaint")
check("7 admin assigns to Suresh", sql("SELECT CONCAT(status,'/',assigned_staff_id) FROM complaints WHERE id=%s" % cid) == "ASSIGNED/6", admin.messages())
hist = sql("SELECT CONCAT(IFNULL(previous_status,'NULL'),'>',new_status) FROM complaint_status_history WHERE complaint_id=%s ORDER BY id" % cid).split("\n")
check("9 history keeps SUBMITTED > UNDER_REVIEW > ASSIGNED (plus priority remark)", hist == ["NULL>SUBMITTED", "SUBMITTED>SUBMITTED", "SUBMITTED>UNDER_REVIEW", "UNDER_REVIEW>ASSIGNED"], hist)
check("7 assignment row recorded", sql("SELECT CONCAT(staff_id,'/',assigned_by,'/',remarks) FROM complaint_assignments WHERE complaint_id=%s" % cid) == "6/1/Isolate the circuit first")
check("13 staff notified of the assignment", sql("SELECT COUNT(*) FROM notifications WHERE user_id=6 AND complaint_id=%s AND title LIKE 'New assignment%%'" % cid) == "1")
admin.form("assignForm").set("staff", "6").submit("Reassign complaint")
check("7 assigning the same staff twice is refused", any("already assigned" in m for m in admin.messages()), admin.messages())

# admin search / filter
admin.get("/admin-complaints.xhtml")
admin.form("filterForm").set("keyword", "Ananya").set("priority", "URGENT").submit("Apply")
rows = re.findall(r'<span class="code">(NIT-[^<]+)</span>', admin.body)
check("14 admin search by reporter + priority filter", code in rows and all(sql("SELECT priority FROM complaints WHERE complaint_code='%s'" % r) == "URGENT" for r in rows), rows)
admin.get("/admin-complaints.xhtml")
admin.form("filterForm").set("staff", "8").submit("Apply")
rows = re.findall(r'<span class="code">(NIT-[^<]+)</span>', admin.body)
check("14 admin filter by assigned staff", rows and all(sql("SELECT assigned_staff_id FROM complaints WHERE complaint_code='%s'" % r) == "8" for r in rows), rows)
admin.get("/admin-complaints.xhtml")
admin.form("filterForm").set("from", "2099-01-01").submit("Apply")
check("14 admin date filter validates future dates", any("cannot be in the future" in m for m in admin.messages()), admin.messages())
admin.get("/admin-complaints.xhtml?status=SUBMITTED")
rows = re.findall(r'<span class="code">(NIT-[^<]+)</span>', admin.body)
check("14 admin status filter via dashboard link", rows and all(sql("SELECT status FROM complaints WHERE complaint_code='%s'" % r) == "SUBMITTED" for r in rows), rows)

# ---------------------------------------------------------------- 10 staff authorization
mohan = login("mohan.kumar@nitte.edu.in", "Staff@123")
mohan.get("/staff-complaint-details.xhtml?id=%s" % cid)
check("10 other staff member cannot open the work order", "Work order not available" in mohan.body and "Sparking power socket" not in mohan.body)
mohan.get("/assigned-complaints.xhtml")
check("10 other staff member's list does not include it", code not in mohan.body)

# ---------------------------------------------------------------- 8 staff progress
suresh.get("/staff-complaint-details.xhtml?id=%s" % cid)
check("staff sees assigned work order with actions", "Start work" in suresh.body and "Isolate the circuit first" in suresh.body)
suresh.form("startForm").set("startNote", "Circuit isolated at the panel.").submit("Start work")
check("8 staff starts work -> IN_PROGRESS", sql("SELECT status FROM complaints WHERE id=%s" % cid) == "IN_PROGRESS", suresh.messages())
suresh.form("updateForm").set("update", "Replacement socket fitted, testing now.").submit("Post update")
check("8 staff posts a remark (status unchanged)", sql("SELECT status FROM complaints WHERE id=%s" % cid) == "IN_PROGRESS" and sql("SELECT COUNT(*) FROM complaint_status_history WHERE complaint_id=%s AND remarks LIKE 'Replacement socket%%'" % cid) == "1")
suresh.form("resolveForm").submit("Mark resolved")
check("15 resolve without description refused", sql("SELECT status FROM complaints WHERE id=%s" % cid) == "IN_PROGRESS" and "describe the work done" in suresh.body)
suresh.get("/staff-complaint-details.xhtml?id=%s" % cid)
suresh.form("resolveForm").set("resolution", "Replaced the damaged socket and verified earthing.").submit("Mark resolved")
check("8 staff marks RESOLVED with resolved_at set", sql("SELECT CONCAT(status,'/',resolved_at IS NOT NULL) FROM complaints WHERE id=%s" % cid) == "RESOLVED/1", suresh.messages())

# ---------------------------------------------------------------- 12 feedback + 13 notifications
ananya.get("/notifications.xhtml")
check("13 student sees status-change notifications", ("%s is now Resolved" % code) in ananya.body and ("%s is now In progress" % code) in ananya.body)
ananya.get("/feedback.xhtml?id=%s" % cid)
ananya.form("feedbackForm").submit("Submit feedback")
check("12 feedback without rating refused", any("rating from 1 to 5" in m for m in ananya.messages()), ananya.messages())
ananya.form("feedbackForm").raw("stars", "5").set("comments", "Quick and safe repair, thank you.").submit("Submit feedback")
check("12 feedback stored", sql("SELECT CONCAT(rating,'/',student_id) FROM feedback WHERE complaint_id=%s" % cid) == "5/2", ananya.messages())
check("12 redirected to details showing the rating", ananya.url.split("?")[0].endswith("/complaint-details.xhtml") and "Quick and safe repair" in ananya.body, ananya.url)
ananya.get("/feedback.xhtml?id=%s" % cid)
check("12 second feedback for the same complaint is not offered", "not waiting for feedback" in ananya.body and 'id="feedbackForm"' not in ananya.body)
rahul.get("/feedback.xhtml?id=%s" % cid)
check("12 another student cannot rate it", 'id="feedbackForm"' not in rahul.body)
check("13 staff notified of the feedback", sql("SELECT COUNT(*) FROM notifications WHERE user_id=6 AND complaint_id=%s AND title LIKE 'Feedback on%%'" % cid) == "1")

# ---------------------------------------------------------------- close
admin.get("/complaint-review.xhtml?id=%s" % cid)
admin.form("closeForm").set("closeNote", "Verified on site.").submit("Close complaint")
check("admin closes RESOLVED complaint", sql("SELECT CONCAT(status,'/',closed_at IS NOT NULL) FROM complaints WHERE id=%s" % cid) == "CLOSED/1", admin.messages())
hist = sql("SELECT new_status FROM complaint_status_history WHERE complaint_id=%s AND previous_status <> new_status ORDER BY id" % cid).split("\n")
check("9 full lifecycle recorded in history", hist == ["UNDER_REVIEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"], hist)
ananya.get("/complaint-details.xhtml?id=%s" % cid)
check("student sees complete history incl. remarks", all(s in ananya.body for s in ["Verified on site.", "Circuit isolated at the panel.", "Replacement socket fitted", "Assigned to Suresh Poojary"]))

# illegal transitions via the service (staff tries to act on a closed complaint)
suresh.get("/staff-complaint-details.xhtml?id=%s" % cid)
check("closed work order has no action forms", 'id="startForm"' not in suresh.body and 'id="resolveForm"' not in suresh.body and 'id="updateForm"' not in suresh.body)

# notification read
before_unread = int(sql("SELECT COUNT(*) FROM notifications WHERE user_id=2 AND is_read=FALSE"))
ananya.get("/notifications.xhtml")
f = [m for m in re.findall(r'<form id="([^"]+)"', ananya.body) if m != "userCard" and m != "notifForm"]
ananya.form(f[0]).submit("Mark all as read")
check("13 mark all notifications read", sql("SELECT COUNT(*) FROM notifications WHERE user_id=2 AND is_read=FALSE") == "0" and before_unread > 0, before_unread)

# ---------------------------------------------------------------- admin management pages
admin.get("/manage-categories.xhtml")
admin.form("categoryForm").set("name", "Parking Area %d" % os.getpid()).set("description", "Parking lots and bays").submit("Add category")
check("admin adds a category", sql("SELECT COUNT(*) FROM categories WHERE name='Parking Area %d'" % os.getpid()) == "1", admin.messages())
admin.get("/manage-categories.xhtml")
admin.form("categoryForm").set("name", "Wi-Fi and Network").submit("Add category")
check("duplicate category name refused", any("already exists" in m for m in admin.messages()), admin.messages())
admin.get("/manage-staff.xhtml")
check("manage staff lists staff only", "Suresh Poojary" in admin.body and "Ananya Rao" not in admin.body)
admin.get("/manage-users.xhtml")
check("manage users lists reporters only", "Ananya Rao" in admin.body and "Suresh Poojary" not in admin.body)

# ---------------------------------------------------------------- reports + CSV
admin.get("/reports.xhtml")
check("reports page renders aggregates", admin.status == 200 and "Monthly submissions" in admin.body and "Staff assignment statistics" in admin.body and "Suresh Poojary" in admin.body)
admin.get("/admin-complaints.xhtml")
f = [m for m in re.findall(r'<form id="([^"]+)"', admin.body) if m not in ("userCard", "filterForm")]
admin.form(f[0]).submit("Export CSV")
check("CSV export downloads", "text/csv" in (admin.headers.get("Content-Type") or "") and code.encode() in admin.raw, admin.headers.get("Content-Type"))

# ---------------------------------------------------------------- every page renders for its role
for who, br, pages in [("student", ananya, ["/student-dashboard.xhtml", "/submit-complaint.xhtml", "/my-complaints.xhtml", "/complaint-details.xhtml?id=1", "/feedback.xhtml", "/notifications.xhtml"]),
                       ("admin", admin, ["/admin-dashboard.xhtml", "/admin-complaints.xhtml", "/complaint-review.xhtml?id=5", "/manage-users.xhtml", "/manage-staff.xhtml", "/manage-categories.xhtml", "/reports.xhtml", "/notifications.xhtml"]),
                       ("staff", suresh, ["/staff-dashboard.xhtml", "/assigned-complaints.xhtml", "/assigned-complaints.xhtml?status=DONE", "/staff-complaint-details.xhtml?id=5", "/notifications.xhtml"])]:
    for p in pages:
        br.get(p)
        ok = br.status == 200 and br.url.split("?")[0].endswith(p.split("?")[0]) and "Something went wrong" not in br.body and "alert-error" not in br.body
        check("page %s renders for %s" % (p, who), ok, (br.status, br.url, br.messages()))

# ---------------------------------------------------------------- logout
ananya.get("/student-dashboard.xhtml")
ananya.form("userCard").submit("Log out")
ananya.get("/student-dashboard.xhtml")
check("logout invalidates the session", ananya.url.endswith("/login.xhtml"), ananya.url)

# ---------------------------------------------------------------- cleanup test data
sql("DELETE FROM users WHERE email='%s'" % email)
sql("DELETE FROM categories WHERE name='Parking Area %d'" % os.getpid())
print("\n%d/%d passed" % (sum(1 for _, ok in results if ok), len(results)))
print("TEST_COMPLAINT_ID", cid, code)
