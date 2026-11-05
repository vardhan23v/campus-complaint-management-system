-- ---------------------------------------------------------------------------
-- Nitte Resolve - demo data
--
-- Run AFTER schema.sql:
--   mysql -u root -p < src/main/resources/sample-data.sql
--
-- Every row has a fixed id and is inserted with INSERT IGNORE, so running the
-- script twice changes nothing. Dates are relative to NOW(), so the
-- dashboards always look current.
--
-- Demo passwords (each stored as a separately salted PBKDF2 hash):
--   administrator          admin@nitte.edu.in          Admin@123
--   students / faculty     ananya.rao@nitte.edu.in     Student@123  (also rahul.shetty, fathima.zahra, kiran.hegde)
--   maintenance staff      suresh.poojary@nitte.edu.in Staff@123    (also mohan.kumar, deepa.nayak)
--
-- Generated complaint codes continue from NIT-2026-00016 for new complaints.
-- ---------------------------------------------------------------------------

USE campus_complaint_db;

-- Users: 1 administrator, 3 students + 1 faculty member (role STUDENT = reporter), 3 maintenance staff.
INSERT IGNORE INTO users (id, name, email, password_hash, role, phone, department, is_active, created_at) VALUES
  (1, 'Priya Menon', 'admin@nitte.edu.in', 'pbkdf2_sha256$210000$89YfgzFb6eWtYIv7/BLY1Q==$0MHbSe+woHDvWKxkt/tccZRE2S7Aa/kuNGHPauVRpwA=', 'ADMIN', '+91 98450 11001', 'Complaint Cell - Estate Office', TRUE, NOW() - INTERVAL 288000 MINUTE),
  (2, 'Ananya Rao', 'ananya.rao@nitte.edu.in', 'pbkdf2_sha256$210000$BmcEq0ALhpqe3KiS3Jq9Hg==$mRuvUMzoBkUY+s/4hfLLboWAp5IzsVMOr4/UBd7fj0s=', 'STUDENT', '+91 98450 22002', 'B.E. Computer Science', TRUE, NOW() - INTERVAL 287940 MINUTE),
  (3, 'Rahul Shetty', 'rahul.shetty@nitte.edu.in', 'pbkdf2_sha256$210000$klhupoCIstcvDZpmgtb1Aw==$BnXPm8SyZZi5eoTM3qIlaJmfWgh8Tk0JZuuVrcQMjMg=', 'STUDENT', '+91 98450 22003', 'B.E. Mechanical', TRUE, NOW() - INTERVAL 287880 MINUTE),
  (4, 'Fathima Zahra', 'fathima.zahra@nitte.edu.in', 'pbkdf2_sha256$210000$Tu3r+Gsz9Js/deLcEmCCew==$ahpNHh9o3qPT6EEJLYTj0/0fz03eE/bWnuDB0YiFjiw=', 'STUDENT', '+91 98450 22004', 'B.E. Electronics & Communication', TRUE, NOW() - INTERVAL 287820 MINUTE),
  (5, 'Dr. Kiran Hegde', 'kiran.hegde@nitte.edu.in', 'pbkdf2_sha256$210000$sR96NSHGp90chD2z+y0M/w==$OXdDQwXLm7FwvJiSIHtgWb8j9tQryZgwttkgnh81VIQ=', 'STUDENT', '+91 98450 33005', 'Faculty - Department of Physics', TRUE, NOW() - INTERVAL 287760 MINUTE),
  (6, 'Suresh Poojary', 'suresh.poojary@nitte.edu.in', 'pbkdf2_sha256$210000$q46SUOjduLjxG94SO9UaEQ==$LU4eIvlgR+GVMSIsCwGg6sfuEl/W/N92wMOiUvezegI=', 'STAFF', '+91 98450 44006', 'Electrical Maintenance', TRUE, NOW() - INTERVAL 287700 MINUTE),
  (7, 'Mohan Kumar', 'mohan.kumar@nitte.edu.in', 'pbkdf2_sha256$210000$zYCnQElD+wwKouko4rWzaQ==$tDoPa7pY4fMCJrMux9lAVNgXA6dRb9XtZ5eT7EAw1/Q=', 'STAFF', '+91 98450 44007', 'Civil & Plumbing', TRUE, NOW() - INTERVAL 287640 MINUTE),
  (8, 'Deepa Nayak', 'deepa.nayak@nitte.edu.in', 'pbkdf2_sha256$210000$3G8LRxNJeWgW5Oa8edFFKw==$VtnaMVkaAna5tdLy0plnRh20y9XPlLE+HPu/QO/o2+o=', 'STAFF', '+91 98450 44008', 'IT & Network Support', TRUE, NOW() - INTERVAL 287580 MINUTE);

-- The twelve default complaint categories.
INSERT IGNORE INTO categories (id, name, description, is_active, created_at) VALUES
  (1, 'Classroom Furniture', 'Benches, desks, chairs, boards and podiums in classrooms.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (2, 'Electrical Issues', 'Switchboards, sockets, wiring, power failures and tripping.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (3, 'Fans and Lighting', 'Ceiling fans, tube lights, corridor and outdoor lighting.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (4, 'Laboratory Equipment', 'Faulty or missing instruments and equipment in laboratories.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (5, 'Wi-Fi and Network', 'Campus Wi-Fi, LAN ports and network connectivity.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (6, 'Water Supply', 'Drinking water, coolers, purifiers and water supply lines.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (7, 'Washroom Maintenance', 'Leaks, blockages, fittings and hygiene in washrooms.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (8, 'Campus Cleanliness', 'Garbage clearance, sweeping and cleanliness of common areas.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (9, 'Library Facilities', 'Reading rooms, furniture, air-conditioning and library systems.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (10, 'Hostel Facilities', 'Rooms, bathrooms, geysers and common areas in hostels.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (11, 'College Transportation', 'College buses, routes, timings and the transport yard.', TRUE, NOW() - INTERVAL 302400 MINUTE),
  (12, 'Other Campus Facilities', 'Anything on campus that does not fit another category.', TRUE, NOW() - INTERVAL 302400 MINUTE);

-- Complaints in every status of the lifecycle.
INSERT IGNORE INTO complaints (id, complaint_code, user_id, category_id, title, description, block, floor, location, priority, status, attachment_path, assigned_staff_id, created_at, updated_at, resolved_at, closed_at) VALUES
  (1, 'NIT-2026-00001', 2, 3, 'Two ceiling fans not working in Room 204', 'Both ceiling fans on the window side of Room 204 do not start even at full regulator. The room gets very hot during afternoon lectures.', 'Block A - Engineering', '2nd floor', 'Room 204', 'HIGH', 'CLOSED', NULL, 6, NOW() - INTERVAL 216000 MINUTE, NOW() - INTERVAL 213120 MINUTE, NOW() - INTERVAL 214440 MINUTE, NOW() - INTERVAL 213120 MINUTE),
  (2, 'NIT-2026-00002', 3, 5, 'Wi-Fi drops every few minutes in the CAD lab', 'The Wi-Fi connection in the CAD lab disconnects every 3-4 minutes, which interrupts licence checks for the design software.', 'Laboratory Complex', '1st floor', 'CAD Lab', 'MEDIUM', 'RESOLVED', NULL, 8, NOW() - INTERVAL 172800 MINUTE, NOW() - INTERVAL 168480 MINUTE, NOW() - INTERVAL 168480 MINUTE, NULL),
  (3, 'NIT-2026-00003', 4, 7, 'Leaking tap and blocked drain in girls'' washroom', 'The second tap from the left leaks continuously and the floor drain is blocked, so water collects across the floor.', 'Block B - Sciences', 'Ground floor', 'Washroom near the main staircase', 'HIGH', 'RESOLVED', NULL, 7, NOW() - INTERVAL 136800 MINUTE, NOW() - INTERVAL 133920 MINUTE, NOW() - INTERVAL 133920 MINUTE, NULL),
  (4, 'NIT-2026-00004', 5, 4, 'Oscilloscope display faulty in Physics Lab 2', 'The display of the dual-channel oscilloscope on bench 6 flickers and loses the trace after a few minutes of use.', 'Block B - Sciences', '1st floor', 'Physics Lab 2, bench 6', 'MEDIUM', 'CLOSED', NULL, 6, NOW() - INTERVAL 115200 MINUTE, NOW() - INTERVAL 105120 MINUTE, NOW() - INTERVAL 106560 MINUTE, NOW() - INTERVAL 105120 MINUTE),
  (5, 'NIT-2026-00005', 2, 2, 'Sparking from the switchboard near the projector', 'There is visible sparking and a burning smell from the switchboard that powers the projector in Seminar Hall 3. We have stopped using it.', 'Block A - Engineering', '3rd floor', 'Seminar Hall 3', 'URGENT', 'IN_PROGRESS', NULL, 6, NOW() - INTERVAL 4320 MINUTE, NOW() - INTERVAL 4200 MINUTE, NULL, NULL),
  (6, 'NIT-2026-00006', 3, 1, 'Broken benches in the last two rows', 'Three benches in the last two rows of Room 112 have broken seat planks and loose bolts.', 'Block C - Management', '1st floor', 'Room 112', 'LOW', 'ASSIGNED', NULL, 7, NOW() - INTERVAL 8640 MINUTE, NOW() - INTERVAL 8340 MINUTE, NULL, NULL),
  (7, 'NIT-2026-00007', 4, 6, 'No drinking water from the 2nd floor cooler', 'The water cooler near Room 210 has not dispensed water since Monday. Students have to go to the ground floor.', 'Block B - Sciences', '2nd floor', 'Water cooler near Room 210', 'HIGH', 'UNDER_REVIEW', NULL, NULL, NOW() - INTERVAL 2880 MINUTE, NOW() - INTERVAL 2700 MINUTE, NULL, NULL),
  (8, 'NIT-2026-00008', 2, 9, 'Reading room air-conditioning not cooling', 'The air-conditioning in the main reading room runs but does not cool. The room is uncomfortable after 11 am.', 'Central Library', '1st floor', 'Main Reading Room', 'MEDIUM', 'SUBMITTED', NULL, NULL, NOW() - INTERVAL 1440 MINUTE, NOW() - INTERVAL 1440 MINUTE, NULL, NULL),
  (9, 'NIT-2026-00009', 5, 5, 'Staff room Wi-Fi access point offline', 'The access point in the Main Block staff room shows no lights and faculty laptops cannot connect.', 'Main Block', 'Ground floor', 'Staff room', 'HIGH', 'IN_PROGRESS', NULL, 8, NOW() - INTERVAL 5760 MINUTE, NOW() - INTERVAL 5460 MINUTE, NULL, NULL),
  (10, 'NIT-2026-00010', 3, 10, 'Hot water geyser not working in hostel bathroom wing B', 'The geyser in bathroom wing B on the 3rd floor of the boys hostel trips the breaker as soon as it is switched on.', 'Boys Hostel', '3rd floor', 'Bathroom wing B', 'MEDIUM', 'SUBMITTED', NULL, NULL, NOW() - INTERVAL 300 MINUTE, NOW() - INTERVAL 300 MINUTE, NULL, NULL),
  (11, 'NIT-2026-00011', 4, 8, 'Garbage not cleared behind the cafeteria', 'The garbage bins at the rear exit of the cafeteria have not been cleared for two days and are attracting stray dogs.', 'Cafeteria', 'Ground floor', 'Rear exit bins', 'MEDIUM', 'RESOLVED', NULL, 7, NOW() - INTERVAL 57600 MINUTE, NOW() - INTERVAL 54720 MINUTE, NOW() - INTERVAL 54720 MINUTE, NULL),
  (12, 'NIT-2026-00012', 2, 11, 'Bus route 7 consistently late in the morning', 'Route 7 has reached campus 15-20 minutes after the first lecture starts on most days this month.', 'Transport Yard', 'Not applicable', 'Route 7 bus', 'LOW', 'CLOSED', NULL, 7, NOW() - INTERVAL 79200 MINUTE, NOW() - INTERVAL 72000 MINUTE, NOW() - INTERVAL 74880 MINUTE, NOW() - INTERVAL 72000 MINUTE),
  (13, 'NIT-2026-00013', 5, 3, 'Corridor lights flickering on the 4th floor', 'Most tube lights in the 4th floor corridor near the lift flicker continuously in the evening.', 'Block A - Engineering', '4th floor', 'Corridor near the lift', 'LOW', 'ASSIGNED', NULL, 6, NOW() - INTERVAL 7200 MINUTE, NOW() - INTERVAL 6840 MINUTE, NULL, NULL),
  (14, 'NIT-2026-00014', 3, 12, 'Basketball court flood lights not switching on', 'None of the flood lights at the basketball court switch on, so evening practice has stopped.', 'Sports Complex', 'Ground floor', 'Basketball court', 'MEDIUM', 'UNDER_REVIEW', NULL, NULL, NOW() - INTERVAL 4320 MINUTE, NOW() - INTERVAL 3840 MINUTE, NULL, NULL),
  (15, 'NIT-2026-00015', 4, 2, 'Loose power socket next to the podium in Room 305', 'The power socket next to the podium is loose and laptop chargers fall out of it during lectures.', 'Block A - Engineering', '3rd floor', 'Room 305, podium', 'MEDIUM', 'RESOLVED', NULL, 6, NOW() - INTERVAL 36000 MINUTE, NOW() - INTERVAL 34380 MINUTE, NOW() - INTERVAL 34380 MINUTE, NULL);

-- Assignments made by the administrator.
INSERT IGNORE INTO complaint_assignments (id, complaint_id, staff_id, assigned_by, assigned_at, remarks) VALUES
  (1, 1, 6, 1, NOW() - INTERVAL 215910 MINUTE, 'Please check the regulators as well.'),
  (2, 2, 8, 1, NOW() - INTERVAL 172620 MINUTE, NULL),
  (3, 3, 7, 1, NOW() - INTERVAL 136740 MINUTE, 'Slippery floor - please treat as high priority.'),
  (4, 4, 6, 1, NOW() - INTERVAL 114960 MINUTE, NULL),
  (5, 5, 6, 1, NOW() - INTERVAL 4285 MINUTE, 'Urgent: isolate and replace.'),
  (6, 6, 7, 1, NOW() - INTERVAL 8340 MINUTE, 'Carpentry work - can be done on Saturday.'),
  (7, 9, 8, 1, NOW() - INTERVAL 5700 MINUTE, NULL),
  (8, 11, 7, 1, NOW() - INTERVAL 57510 MINUTE, NULL),
  (9, 12, 7, 1, NOW() - INTERVAL 78780 MINUTE, 'Coordinate with the transport office.'),
  (10, 13, 6, 1, NOW() - INTERVAL 6840 MINUTE, 'Replace with LED battens if stock is available.'),
  (11, 15, 6, 1, NOW() - INTERVAL 35850 MINUTE, NULL);

-- Append-only status history (previous_status = new_status marks a remark without a status change).
INSERT IGNORE INTO complaint_status_history (id, complaint_id, previous_status, new_status, updated_by, remarks, created_at) VALUES
  (1, 1, NULL, 'SUBMITTED', 2, 'Complaint submitted by Ananya Rao.', NOW() - INTERVAL 216000 MINUTE),
  (2, 1, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 215950 MINUTE),
  (3, 1, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Suresh Poojary (Electrical Maintenance): Please check the regulators as well.', NOW() - INTERVAL 215910 MINUTE),
  (4, 1, 'ASSIGNED', 'IN_PROGRESS', 6, 'Inspected - both capacitors have failed.', NOW() - INTERVAL 215700 MINUTE),
  (5, 1, 'IN_PROGRESS', 'RESOLVED', 6, 'Replaced two fan capacitors and one faulty regulator. Both fans tested at all speeds.', NOW() - INTERVAL 214440 MINUTE),
  (6, 1, 'RESOLVED', 'CLOSED', 1, 'Verified with the class representative.', NOW() - INTERVAL 213120 MINUTE),
  (7, 2, NULL, 'SUBMITTED', 3, 'Complaint submitted by Rahul Shetty.', NOW() - INTERVAL 172800 MINUTE),
  (8, 2, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 172680 MINUTE),
  (9, 2, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Deepa Nayak (IT & Network Support).', NOW() - INTERVAL 172620 MINUTE),
  (10, 2, 'ASSIGNED', 'IN_PROGRESS', 8, 'Access point logs show channel interference.', NOW() - INTERVAL 171480 MINUTE),
  (11, 2, 'IN_PROGRESS', 'IN_PROGRESS', 8, 'Moved the access point to channel 44 and updated its firmware. Monitoring for a day.', NOW() - INTERVAL 170400 MINUTE),
  (12, 2, 'IN_PROGRESS', 'RESOLVED', 8, 'Connection stable for 24 hours after the channel change and firmware update.', NOW() - INTERVAL 168480 MINUTE),
  (13, 3, NULL, 'SUBMITTED', 4, 'Complaint submitted by Fathima Zahra.', NOW() - INTERVAL 136800 MINUTE),
  (14, 3, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 136770 MINUTE),
  (15, 3, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Mohan Kumar (Civil & Plumbing): Slippery floor - please treat as high priority.', NOW() - INTERVAL 136740 MINUTE),
  (16, 3, 'ASSIGNED', 'IN_PROGRESS', 7, 'Work started by Mohan Kumar.', NOW() - INTERVAL 136620 MINUTE),
  (17, 3, 'IN_PROGRESS', 'RESOLVED', 7, 'Replaced the tap cartridge and cleared the drain line. Floor dried and cleaned.', NOW() - INTERVAL 133920 MINUTE),
  (18, 4, NULL, 'SUBMITTED', 5, 'Complaint submitted by Dr. Kiran Hegde.', NOW() - INTERVAL 115200 MINUTE),
  (19, 4, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 115020 MINUTE),
  (20, 4, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Suresh Poojary (Electrical Maintenance).', NOW() - INTERVAL 114960 MINUTE),
  (21, 4, 'ASSIGNED', 'IN_PROGRESS', 6, 'Unit taken to the electronics workshop.', NOW() - INTERVAL 112320 MINUTE),
  (22, 4, 'IN_PROGRESS', 'RESOLVED', 6, 'Replaced the display power board. Calibrated and returned to bench 6.', NOW() - INTERVAL 106560 MINUTE),
  (23, 4, 'RESOLVED', 'CLOSED', 1, 'Resolution verified. Complaint closed by the complaint cell.', NOW() - INTERVAL 105120 MINUTE),
  (24, 5, NULL, 'SUBMITTED', 2, 'Complaint submitted by Ananya Rao.', NOW() - INTERVAL 4320 MINUTE),
  (25, 5, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Safety risk - power to the board isolated.', NOW() - INTERVAL 4300 MINUTE),
  (26, 5, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Suresh Poojary (Electrical Maintenance): Urgent: isolate and replace.', NOW() - INTERVAL 4285 MINUTE),
  (27, 5, 'ASSIGNED', 'IN_PROGRESS', 6, 'Board isolated at the distribution panel. Replacement switches ordered.', NOW() - INTERVAL 4200 MINUTE),
  (28, 5, 'IN_PROGRESS', 'IN_PROGRESS', 6, 'New modular switches arrived; installation scheduled for tomorrow morning.', NOW() - INTERVAL 2520 MINUTE),
  (29, 6, NULL, 'SUBMITTED', 3, 'Complaint submitted by Rahul Shetty.', NOW() - INTERVAL 8640 MINUTE),
  (30, 6, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 8400 MINUTE),
  (31, 6, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Mohan Kumar (Civil & Plumbing): Carpentry work - can be done on Saturday.', NOW() - INTERVAL 8340 MINUTE),
  (32, 7, NULL, 'SUBMITTED', 4, 'Complaint submitted by Fathima Zahra.', NOW() - INTERVAL 2880 MINUTE),
  (33, 7, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Checking whether the purifier service contract covers this unit.', NOW() - INTERVAL 2700 MINUTE),
  (34, 8, NULL, 'SUBMITTED', 2, 'Complaint submitted by Ananya Rao.', NOW() - INTERVAL 1440 MINUTE),
  (35, 9, NULL, 'SUBMITTED', 5, 'Complaint submitted by Dr. Kiran Hegde.', NOW() - INTERVAL 5760 MINUTE),
  (36, 9, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 5720 MINUTE),
  (37, 9, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Deepa Nayak (IT & Network Support).', NOW() - INTERVAL 5700 MINUTE),
  (38, 9, 'ASSIGNED', 'IN_PROGRESS', 8, 'PoE switch port found dead; patched to a spare port temporarily.', NOW() - INTERVAL 5460 MINUTE),
  (39, 10, NULL, 'SUBMITTED', 3, 'Complaint submitted by Rahul Shetty.', NOW() - INTERVAL 300 MINUTE),
  (40, 11, NULL, 'SUBMITTED', 4, 'Complaint submitted by Fathima Zahra.', NOW() - INTERVAL 57600 MINUTE),
  (41, 11, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 57540 MINUTE),
  (42, 11, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Mohan Kumar (Civil & Plumbing).', NOW() - INTERVAL 57510 MINUTE),
  (43, 11, 'ASSIGNED', 'IN_PROGRESS', 7, 'Work started by Mohan Kumar.', NOW() - INTERVAL 57360 MINUTE),
  (44, 11, 'IN_PROGRESS', 'RESOLVED', 7, 'Bins cleared and area sanitised. Housekeeping schedule changed to twice daily.', NOW() - INTERVAL 54720 MINUTE),
  (45, 12, NULL, 'SUBMITTED', 2, 'Complaint submitted by Ananya Rao.', NOW() - INTERVAL 79200 MINUTE),
  (46, 12, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 78840 MINUTE),
  (47, 12, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Mohan Kumar (Civil & Plumbing): Coordinate with the transport office.', NOW() - INTERVAL 78780 MINUTE),
  (48, 12, 'ASSIGNED', 'IN_PROGRESS', 7, 'Work started by Mohan Kumar.', NOW() - INTERVAL 77760 MINUTE),
  (49, 12, 'IN_PROGRESS', 'RESOLVED', 7, 'Route 7 departure moved 15 minutes earlier after discussion with the transport office.', NOW() - INTERVAL 74880 MINUTE),
  (50, 12, 'RESOLVED', 'CLOSED', 1, 'Resolution verified. Complaint closed by the complaint cell.', NOW() - INTERVAL 72000 MINUTE),
  (51, 13, NULL, 'SUBMITTED', 5, 'Complaint submitted by Dr. Kiran Hegde.', NOW() - INTERVAL 7200 MINUTE),
  (52, 13, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 6900 MINUTE),
  (53, 13, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Suresh Poojary (Electrical Maintenance): Replace with LED battens if stock is available.', NOW() - INTERVAL 6840 MINUTE),
  (54, 14, NULL, 'SUBMITTED', 3, 'Complaint submitted by Rahul Shetty.', NOW() - INTERVAL 4320 MINUTE),
  (55, 14, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 3840 MINUTE),
  (56, 15, NULL, 'SUBMITTED', 4, 'Complaint submitted by Fathima Zahra.', NOW() - INTERVAL 36000 MINUTE),
  (57, 15, 'SUBMITTED', 'UNDER_REVIEW', 1, 'Reviewed by the complaint cell.', NOW() - INTERVAL 35880 MINUTE),
  (58, 15, 'UNDER_REVIEW', 'ASSIGNED', 1, 'Assigned to Suresh Poojary (Electrical Maintenance).', NOW() - INTERVAL 35850 MINUTE),
  (59, 15, 'ASSIGNED', 'IN_PROGRESS', 6, 'Work started by Suresh Poojary.', NOW() - INTERVAL 34560 MINUTE),
  (60, 15, 'IN_PROGRESS', 'RESOLVED', 6, 'Replaced the socket and fixed the faceplate firmly.', NOW() - INTERVAL 34380 MINUTE);

-- Feedback on resolved / closed complaints.
INSERT IGNORE INTO feedback (id, complaint_id, student_id, rating, comments, created_at) VALUES
  (1, 1, 2, 5, 'Fixed within a day. Thank you!', NOW() - INTERVAL 212940 MINUTE),
  (2, 2, 3, 4, 'Works well now, though it took a few days.', NOW() - INTERVAL 168300 MINUTE),
  (3, 4, 5, 3, 'Repaired, but a spare unit during the repair would have helped the lab sessions.', NOW() - INTERVAL 104940 MINUTE),
  (4, 11, 4, 4, 'Much cleaner now.', NOW() - INTERVAL 54540 MINUTE),
  (5, 12, 2, 2, 'Better for a week, then late again.', NOW() - INTERVAL 71820 MINUTE);

-- Notifications: anything older than three days is already read.
INSERT IGNORE INTO notifications (id, user_id, complaint_id, title, message, is_read, created_at) VALUES
  (1, 2, 1, 'Complaint NIT-2026-00001 received', 'We have received "Two ceiling fans not working in Room 204". You will be notified at every status change.', TRUE, NOW() - INTERVAL 216000 MINUTE),
  (2, 1, 1, 'New complaint NIT-2026-00001', 'Fans and Lighting · Block A - Engineering, Room 204 · High priority', TRUE, NOW() - INTERVAL 216000 MINUTE),
  (3, 2, 1, 'NIT-2026-00001 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 215950 MINUTE),
  (4, 2, 1, 'NIT-2026-00001 is now Assigned', 'Your complaint has been assigned to Suresh Poojary of the maintenance team.', TRUE, NOW() - INTERVAL 215910 MINUTE),
  (5, 6, 1, 'New assignment NIT-2026-00001', 'Two ceiling fans not working in Room 204 · Block A - Engineering · 2nd floor · Room 204 · High priority', TRUE, NOW() - INTERVAL 215910 MINUTE),
  (6, 2, 1, 'NIT-2026-00001 is now In progress', 'Inspected - both capacitors have failed.', TRUE, NOW() - INTERVAL 215700 MINUTE),
  (7, 2, 1, 'NIT-2026-00001 is now Resolved', 'Replaced two fan capacitors and one faulty regulator. Both fans tested at all speeds.', TRUE, NOW() - INTERVAL 214440 MINUTE),
  (8, 2, 1, 'NIT-2026-00001 is now Closed', 'Verified with the class representative.', TRUE, NOW() - INTERVAL 213120 MINUTE),
  (9, 6, 1, 'Feedback on NIT-2026-00001: ★★★★★', 'Fixed within a day. Thank you!', TRUE, NOW() - INTERVAL 212940 MINUTE),
  (10, 3, 2, 'Complaint NIT-2026-00002 received', 'We have received "Wi-Fi drops every few minutes in the CAD lab". You will be notified at every status change.', TRUE, NOW() - INTERVAL 172800 MINUTE),
  (11, 1, 2, 'New complaint NIT-2026-00002', 'Wi-Fi and Network · Laboratory Complex, CAD Lab · Medium priority', TRUE, NOW() - INTERVAL 172800 MINUTE),
  (12, 3, 2, 'NIT-2026-00002 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 172680 MINUTE),
  (13, 3, 2, 'NIT-2026-00002 is now Assigned', 'Your complaint has been assigned to Deepa Nayak of the maintenance team.', TRUE, NOW() - INTERVAL 172620 MINUTE),
  (14, 8, 2, 'New assignment NIT-2026-00002', 'Wi-Fi drops every few minutes in the CAD lab · Laboratory Complex · 1st floor · CAD Lab · Medium priority', TRUE, NOW() - INTERVAL 172620 MINUTE),
  (15, 3, 2, 'NIT-2026-00002 is now In progress', 'Access point logs show channel interference.', TRUE, NOW() - INTERVAL 171480 MINUTE),
  (16, 3, 2, 'Work update on NIT-2026-00002', 'Moved the access point to channel 44 and updated its firmware. Monitoring for a day.', TRUE, NOW() - INTERVAL 170400 MINUTE),
  (17, 3, 2, 'NIT-2026-00002 is now Resolved', 'Connection stable for 24 hours after the channel change and firmware update.', TRUE, NOW() - INTERVAL 168480 MINUTE),
  (18, 8, 2, 'Feedback on NIT-2026-00002: ★★★★☆', 'Works well now, though it took a few days.', TRUE, NOW() - INTERVAL 168300 MINUTE),
  (19, 4, 3, 'Complaint NIT-2026-00003 received', 'We have received "Leaking tap and blocked drain in girls'' washroom". You will be notified at every status change.', TRUE, NOW() - INTERVAL 136800 MINUTE),
  (20, 1, 3, 'New complaint NIT-2026-00003', 'Washroom Maintenance · Block B - Sciences, Washroom near the main staircase · High priority', TRUE, NOW() - INTERVAL 136800 MINUTE),
  (21, 4, 3, 'NIT-2026-00003 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 136770 MINUTE),
  (22, 4, 3, 'NIT-2026-00003 is now Assigned', 'Your complaint has been assigned to Mohan Kumar of the maintenance team.', TRUE, NOW() - INTERVAL 136740 MINUTE),
  (23, 7, 3, 'New assignment NIT-2026-00003', 'Leaking tap and blocked drain in girls'' washroom · Block B - Sciences · Ground floor · Washroom near the main staircase · High priority', TRUE, NOW() - INTERVAL 136740 MINUTE),
  (24, 4, 3, 'NIT-2026-00003 is now In progress', 'Work started by Mohan Kumar.', TRUE, NOW() - INTERVAL 136620 MINUTE),
  (25, 4, 3, 'NIT-2026-00003 is now Resolved', 'Replaced the tap cartridge and cleared the drain line. Floor dried and cleaned.', TRUE, NOW() - INTERVAL 133920 MINUTE),
  (26, 5, 4, 'Complaint NIT-2026-00004 received', 'We have received "Oscilloscope display faulty in Physics Lab 2". You will be notified at every status change.', TRUE, NOW() - INTERVAL 115200 MINUTE),
  (27, 1, 4, 'New complaint NIT-2026-00004', 'Laboratory Equipment · Block B - Sciences, Physics Lab 2, bench 6 · Medium priority', TRUE, NOW() - INTERVAL 115200 MINUTE),
  (28, 5, 4, 'NIT-2026-00004 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 115020 MINUTE),
  (29, 5, 4, 'NIT-2026-00004 is now Assigned', 'Your complaint has been assigned to Suresh Poojary of the maintenance team.', TRUE, NOW() - INTERVAL 114960 MINUTE),
  (30, 6, 4, 'New assignment NIT-2026-00004', 'Oscilloscope display faulty in Physics Lab 2 · Block B - Sciences · 1st floor · Physics Lab 2, bench 6 · Medium priority', TRUE, NOW() - INTERVAL 114960 MINUTE),
  (31, 5, 4, 'NIT-2026-00004 is now In progress', 'Unit taken to the electronics workshop.', TRUE, NOW() - INTERVAL 112320 MINUTE),
  (32, 5, 4, 'NIT-2026-00004 is now Resolved', 'Replaced the display power board. Calibrated and returned to bench 6.', TRUE, NOW() - INTERVAL 106560 MINUTE),
  (33, 5, 4, 'NIT-2026-00004 is now Closed', 'Resolution verified. Complaint closed by the complaint cell.', TRUE, NOW() - INTERVAL 105120 MINUTE),
  (34, 6, 4, 'Feedback on NIT-2026-00004: ★★★☆☆', 'Repaired, but a spare unit during the repair would have helped the lab sessions.', TRUE, NOW() - INTERVAL 104940 MINUTE),
  (35, 2, 12, 'Complaint NIT-2026-00012 received', 'We have received "Bus route 7 consistently late in the morning". You will be notified at every status change.', TRUE, NOW() - INTERVAL 79200 MINUTE),
  (36, 1, 12, 'New complaint NIT-2026-00012', 'College Transportation · Transport Yard, Route 7 bus · Low priority', TRUE, NOW() - INTERVAL 79200 MINUTE),
  (37, 2, 12, 'NIT-2026-00012 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 78840 MINUTE),
  (38, 2, 12, 'NIT-2026-00012 is now Assigned', 'Your complaint has been assigned to Mohan Kumar of the maintenance team.', TRUE, NOW() - INTERVAL 78780 MINUTE),
  (39, 7, 12, 'New assignment NIT-2026-00012', 'Bus route 7 consistently late in the morning · Transport Yard · Not applicable · Route 7 bus · Low priority', TRUE, NOW() - INTERVAL 78780 MINUTE),
  (40, 2, 12, 'NIT-2026-00012 is now In progress', 'Work started by Mohan Kumar.', TRUE, NOW() - INTERVAL 77760 MINUTE),
  (41, 2, 12, 'NIT-2026-00012 is now Resolved', 'Route 7 departure moved 15 minutes earlier after discussion with the transport office.', TRUE, NOW() - INTERVAL 74880 MINUTE),
  (42, 2, 12, 'NIT-2026-00012 is now Closed', 'Resolution verified. Complaint closed by the complaint cell.', TRUE, NOW() - INTERVAL 72000 MINUTE),
  (43, 7, 12, 'Feedback on NIT-2026-00012: ★★☆☆☆', 'Better for a week, then late again.', TRUE, NOW() - INTERVAL 71820 MINUTE),
  (44, 4, 11, 'Complaint NIT-2026-00011 received', 'We have received "Garbage not cleared behind the cafeteria". You will be notified at every status change.', TRUE, NOW() - INTERVAL 57600 MINUTE),
  (45, 1, 11, 'New complaint NIT-2026-00011', 'Campus Cleanliness · Cafeteria, Rear exit bins · Medium priority', TRUE, NOW() - INTERVAL 57600 MINUTE),
  (46, 4, 11, 'NIT-2026-00011 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 57540 MINUTE),
  (47, 4, 11, 'NIT-2026-00011 is now Assigned', 'Your complaint has been assigned to Mohan Kumar of the maintenance team.', TRUE, NOW() - INTERVAL 57510 MINUTE),
  (48, 7, 11, 'New assignment NIT-2026-00011', 'Garbage not cleared behind the cafeteria · Cafeteria · Ground floor · Rear exit bins · Medium priority', TRUE, NOW() - INTERVAL 57510 MINUTE),
  (49, 4, 11, 'NIT-2026-00011 is now In progress', 'Work started by Mohan Kumar.', TRUE, NOW() - INTERVAL 57360 MINUTE),
  (50, 4, 11, 'NIT-2026-00011 is now Resolved', 'Bins cleared and area sanitised. Housekeeping schedule changed to twice daily.', TRUE, NOW() - INTERVAL 54720 MINUTE),
  (51, 7, 11, 'Feedback on NIT-2026-00011: ★★★★☆', 'Much cleaner now.', TRUE, NOW() - INTERVAL 54540 MINUTE),
  (52, 4, 15, 'Complaint NIT-2026-00015 received', 'We have received "Loose power socket next to the podium in Room 305". You will be notified at every status change.', TRUE, NOW() - INTERVAL 36000 MINUTE),
  (53, 1, 15, 'New complaint NIT-2026-00015', 'Electrical Issues · Block A - Engineering, Room 305, podium · Medium priority', TRUE, NOW() - INTERVAL 36000 MINUTE),
  (54, 4, 15, 'NIT-2026-00015 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 35880 MINUTE),
  (55, 4, 15, 'NIT-2026-00015 is now Assigned', 'Your complaint has been assigned to Suresh Poojary of the maintenance team.', TRUE, NOW() - INTERVAL 35850 MINUTE),
  (56, 6, 15, 'New assignment NIT-2026-00015', 'Loose power socket next to the podium in Room 305 · Block A - Engineering · 3rd floor · Room 305, podium · Medium priority', TRUE, NOW() - INTERVAL 35850 MINUTE),
  (57, 4, 15, 'NIT-2026-00015 is now In progress', 'Work started by Suresh Poojary.', TRUE, NOW() - INTERVAL 34560 MINUTE),
  (58, 4, 15, 'NIT-2026-00015 is now Resolved', 'Replaced the socket and fixed the faceplate firmly.', TRUE, NOW() - INTERVAL 34380 MINUTE),
  (59, 3, 6, 'Complaint NIT-2026-00006 received', 'We have received "Broken benches in the last two rows". You will be notified at every status change.', TRUE, NOW() - INTERVAL 8640 MINUTE),
  (60, 1, 6, 'New complaint NIT-2026-00006', 'Classroom Furniture · Block C - Management, Room 112 · Low priority', TRUE, NOW() - INTERVAL 8640 MINUTE),
  (61, 3, 6, 'NIT-2026-00006 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 8400 MINUTE),
  (62, 3, 6, 'NIT-2026-00006 is now Assigned', 'Your complaint has been assigned to Mohan Kumar of the maintenance team.', TRUE, NOW() - INTERVAL 8340 MINUTE),
  (63, 7, 6, 'New assignment NIT-2026-00006', 'Broken benches in the last two rows · Block C - Management · 1st floor · Room 112 · Low priority', TRUE, NOW() - INTERVAL 8340 MINUTE),
  (64, 5, 13, 'Complaint NIT-2026-00013 received', 'We have received "Corridor lights flickering on the 4th floor". You will be notified at every status change.', TRUE, NOW() - INTERVAL 7200 MINUTE),
  (65, 1, 13, 'New complaint NIT-2026-00013', 'Fans and Lighting · Block A - Engineering, Corridor near the lift · Low priority', TRUE, NOW() - INTERVAL 7200 MINUTE),
  (66, 5, 13, 'NIT-2026-00013 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 6900 MINUTE),
  (67, 5, 13, 'NIT-2026-00013 is now Assigned', 'Your complaint has been assigned to Suresh Poojary of the maintenance team.', TRUE, NOW() - INTERVAL 6840 MINUTE),
  (68, 6, 13, 'New assignment NIT-2026-00013', 'Corridor lights flickering on the 4th floor · Block A - Engineering · 4th floor · Corridor near the lift · Low priority', TRUE, NOW() - INTERVAL 6840 MINUTE),
  (69, 5, 9, 'Complaint NIT-2026-00009 received', 'We have received "Staff room Wi-Fi access point offline". You will be notified at every status change.', TRUE, NOW() - INTERVAL 5760 MINUTE),
  (70, 1, 9, 'New complaint NIT-2026-00009', 'Wi-Fi and Network · Main Block, Staff room · High priority', TRUE, NOW() - INTERVAL 5760 MINUTE),
  (71, 5, 9, 'NIT-2026-00009 is now Under review', 'Reviewed by the complaint cell.', TRUE, NOW() - INTERVAL 5720 MINUTE),
  (72, 5, 9, 'NIT-2026-00009 is now Assigned', 'Your complaint has been assigned to Deepa Nayak of the maintenance team.', TRUE, NOW() - INTERVAL 5700 MINUTE),
  (73, 8, 9, 'New assignment NIT-2026-00009', 'Staff room Wi-Fi access point offline · Main Block · Ground floor · Staff room · High priority', TRUE, NOW() - INTERVAL 5700 MINUTE),
  (74, 5, 9, 'NIT-2026-00009 is now In progress', 'PoE switch port found dead; patched to a spare port temporarily.', TRUE, NOW() - INTERVAL 5460 MINUTE),
  (75, 2, 5, 'Complaint NIT-2026-00005 received', 'We have received "Sparking from the switchboard near the projector". You will be notified at every status change.', FALSE, NOW() - INTERVAL 4320 MINUTE),
  (76, 1, 5, 'New complaint NIT-2026-00005', 'Electrical Issues · Block A - Engineering, Seminar Hall 3 · Urgent priority', FALSE, NOW() - INTERVAL 4320 MINUTE),
  (77, 3, 14, 'Complaint NIT-2026-00014 received', 'We have received "Basketball court flood lights not switching on". You will be notified at every status change.', FALSE, NOW() - INTERVAL 4320 MINUTE),
  (78, 1, 14, 'New complaint NIT-2026-00014', 'Other Campus Facilities · Sports Complex, Basketball court · Medium priority', FALSE, NOW() - INTERVAL 4320 MINUTE),
  (79, 2, 5, 'NIT-2026-00005 is now Under review', 'Safety risk - power to the board isolated.', FALSE, NOW() - INTERVAL 4300 MINUTE),
  (80, 2, 5, 'NIT-2026-00005 is now Assigned', 'Your complaint has been assigned to Suresh Poojary of the maintenance team.', FALSE, NOW() - INTERVAL 4285 MINUTE),
  (81, 6, 5, 'New assignment NIT-2026-00005', 'Sparking from the switchboard near the projector · Block A - Engineering · 3rd floor · Seminar Hall 3 · Urgent priority', FALSE, NOW() - INTERVAL 4285 MINUTE),
  (82, 2, 5, 'NIT-2026-00005 is now In progress', 'Board isolated at the distribution panel. Replacement switches ordered.', FALSE, NOW() - INTERVAL 4200 MINUTE),
  (83, 3, 14, 'NIT-2026-00014 is now Under review', 'Reviewed by the complaint cell.', FALSE, NOW() - INTERVAL 3840 MINUTE),
  (84, 4, 7, 'Complaint NIT-2026-00007 received', 'We have received "No drinking water from the 2nd floor cooler". You will be notified at every status change.', FALSE, NOW() - INTERVAL 2880 MINUTE),
  (85, 1, 7, 'New complaint NIT-2026-00007', 'Water Supply · Block B - Sciences, Water cooler near Room 210 · High priority', FALSE, NOW() - INTERVAL 2880 MINUTE),
  (86, 4, 7, 'NIT-2026-00007 is now Under review', 'Checking whether the purifier service contract covers this unit.', FALSE, NOW() - INTERVAL 2700 MINUTE),
  (87, 2, 5, 'Work update on NIT-2026-00005', 'New modular switches arrived; installation scheduled for tomorrow morning.', FALSE, NOW() - INTERVAL 2520 MINUTE),
  (88, 2, 8, 'Complaint NIT-2026-00008 received', 'We have received "Reading room air-conditioning not cooling". You will be notified at every status change.', FALSE, NOW() - INTERVAL 1440 MINUTE),
  (89, 1, 8, 'New complaint NIT-2026-00008', 'Library Facilities · Central Library, Main Reading Room · Medium priority', FALSE, NOW() - INTERVAL 1440 MINUTE),
  (90, 3, 10, 'Complaint NIT-2026-00010 received', 'We have received "Hot water geyser not working in hostel bathroom wing B". You will be notified at every status change.', FALSE, NOW() - INTERVAL 300 MINUTE),
  (91, 1, 10, 'New complaint NIT-2026-00010', 'Hostel Facilities · Boys Hostel, Bathroom wing B · Medium priority', FALSE, NOW() - INTERVAL 300 MINUTE);

-- New complaints continue after the demo rows (NIT-2026-00016 ...).
ALTER TABLE complaints AUTO_INCREMENT = 16;
