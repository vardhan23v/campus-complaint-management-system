"""Minimal JSF browser: cookies, form parsing, urlencoded + multipart posts."""
import os, re, html, uuid, urllib.request, urllib.parse, http.cookiejar

# Override with e.g. CC_BASE_URL=http://localhost:8086/campus-complaint
BASE = os.environ.get("CC_BASE_URL", "http://localhost:8080/campus-complaint")

class Browser:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.url = None; self.body = ""; self.status = None

    def get(self, path):
        return self._open(urllib.request.Request(BASE + path if path.startswith("/") else path))

    def _open(self, req):
        try:
            r = self.opener.open(req)
            self.status = r.status; self.url = r.geturl()
            raw = r.read(); self.headers = r.headers
        except urllib.error.HTTPError as e:
            self.status = e.code; self.url = e.geturl(); raw = e.read(); self.headers = e.headers
        try: self.body = raw.decode("utf-8")
        except UnicodeDecodeError: self.body = ""; self.raw = raw
        self.raw = raw
        return self

    def form(self, form_id):
        m = re.search(r'<form[^>]*id="%s"[^>]*>(.*?)</form>' % re.escape(form_id), self.body, re.S)
        assert m, "form %s not found on %s" % (form_id, self.url)
        tag = re.search(r'<form[^>]*id="%s"[^>]*>' % re.escape(form_id), self.body).group(0)
        action = html.unescape(re.search(r'action="([^"]*)"', tag).group(1))
        multipart = "multipart" in tag
        inner = m.group(1); fields = {}; buttons = {}
        for inp in re.findall(r'<input[^>]*>', inner):
            name = re.search(r'name="([^"]*)"', inp); 
            if not name: continue
            name = html.unescape(name.group(1)); typ = (re.search(r'type="([^"]*)"', inp) or [None, "text"])[1]
            val = re.search(r'value="([^"]*)"', inp); val = html.unescape(val.group(1)) if val else ""
            if typ == "submit": buttons[val] = name
            elif typ in ("checkbox", "radio"):
                if "checked" in inp: fields[name] = val
            elif typ != "file": fields[name] = val
        for ta in re.finditer(r'<textarea[^>]*name="([^"]*)"[^>]*>(.*?)</textarea>', inner, re.S):
            fields[html.unescape(ta.group(1))] = html.unescape(ta.group(2))
        for sel in re.finditer(r'<select[^>]*name="([^"]*)"[^>]*>(.*?)</select>', inner, re.S):
            opt = re.search(r'<option[^>]*value="([^"]*)"[^>]*selected', sel.group(2))
            fields[html.unescape(sel.group(1))] = html.unescape(opt.group(1)) if opt else ""
        return Form(self, action, fields, buttons, multipart, form_id)

    def text(self):
        t = re.sub(r'<script.*?</script>|<style.*?</style>', '', self.body, flags=re.S)
        return html.unescape(re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', ' ', t)))

    def messages(self):
        return [html.unescape(re.sub(r'<[^>]+>', '', m)).strip() for m in re.findall(r'<li class="alert[^"]*">(.*?)</li>', self.body, re.S)]

    def field_errors(self):
        return [html.unescape(m).strip() for m in re.findall(r'class="field-error"[^>]*>([^<]+)<', self.body)]

class Form:
    def __init__(self, b, action, fields, buttons, multipart, fid):
        self.b = b; self.action = action; self.fields = fields; self.buttons = buttons; self.multipart = multipart; self.files = {}; self.id = fid
    def set(self, short, value):
        self.fields[self.id + ":" + short if ":" not in short and short not in self.fields else short] = value; return self
    def raw(self, name, value):
        self.fields[name] = value; return self
    def file(self, short, filename, content, ctype):
        self.files[self.id + ":" + short] = (filename, content, ctype); return self
    def submit(self, button_label):
        assert button_label in self.buttons, "button %r not in %s" % (button_label, list(self.buttons))
        data = dict(self.fields); data[self.buttons[button_label]] = button_label
        url = urllib.parse.urljoin(BASE + "/", self.action)
        if self.multipart:
            boundary = uuid.uuid4().hex; parts = []
            for k, v in data.items():
                parts.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n" % (boundary, k, v)).encode())
            for k, (fn, content, ct) in self.files.items():
                parts.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n" % (boundary, k, fn, ct)).encode() + content + b"\r\n")
            if not self.files:
                for k in re.findall(r'<input[^>]*type="file"[^>]*name="([^"]*)"', self.b.body):
                    parts.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"\"\r\nContent-Type: application/octet-stream\r\n\r\n\r\n" % (boundary, k)).encode())
            body = b"".join(parts) + ("--%s--\r\n" % boundary).encode()
            req = urllib.request.Request(url, data=body, headers={"Content-Type": "multipart/form-data; boundary=" + boundary})
        else:
            req = urllib.request.Request(url, data=urllib.parse.urlencode(data).encode(), headers={"Content-Type": "application/x-www-form-urlencoded"})
        return self.b._open(req)

def login(email, password):
    b = Browser(); b.get("/login.xhtml")
    b.form("loginForm").set("email", email).set("password", password).submit("Log in")
    return b
