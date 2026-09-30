package lk.catchbus.api.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "catchbus-backend", "port", 8081);
    }

    @GetMapping(value = "/", produces = "text/html")
    public String index() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>CatchBusLK Operations API (Port 8081)</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0b130e; color: #e2e8f0; margin: 0; padding: 40px 20px; line-height: 1.6; }
                .container { max-width: 820px; margin: 0 auto; background: #132219; border: 1px solid rgba(94, 234, 212, 0.2); border-radius: 16px; padding: 32px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                .badge { display: inline-block; background: rgba(52, 211, 153, 0.15); color: #34d399; font-weight: bold; font-size: 12px; padding: 4px 12px; border-radius: 9999px; text-transform: uppercase; letter-spacing: 1px; border: 1px solid rgba(52, 211, 153, 0.3); }
                h1 { color: #ffffff; margin-top: 12px; margin-bottom: 8px; font-size: 28px; }
                p.lead { color: #94a3b8; font-size: 15px; margin-top: 0; }
                .card { background: #0f1b14; border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 20px; margin-top: 20px; }
                h3 { margin-top: 0; color: #5eead4; font-size: 16px; text-transform: uppercase; letter-spacing: 0.5px; }
                table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px; }
                th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
                th { color: #94a3b8; font-weight: 600; }
                code { background: rgba(0,0,0,0.4); padding: 2px 6px; border-radius: 4px; font-family: Consolas, monospace; color: #7dd3fc; font-size: 13px; }
                a { color: #38bdf8; text-decoration: none; font-weight: 600; }
                a:hover { text-decoration: underline; }
                .note { background: rgba(56, 189, 248, 0.08); border-left: 4px solid #38bdf8; padding: 12px 16px; border-radius: 4px; font-size: 14px; margin-top: 24px; color: #bae6fd; }
              </style>
            </head>
            <body>
              <div class="container">
                <span class="badge">&#9679; Service Online &middot; Port 8081</span>
                <h1>CatchBusLK QR Staff &amp; Conductor API</h1>
                <p class="lead">Backend REST API module for Staff Authentication, QR Ticket Verification, Live GPS Tracking, and RBAC.</p>
                
                <div class="note">
                  &#8505;&#65039; <strong>Information:</strong> This service is the REST API backend. It provides JSON endpoints for the <strong>React Frontend (Port 5173)</strong>.
                </div>

                <div class="card">
                  <h3>Verified Demo Accounts</h3>
                  <table>
                    <thead>
                      <tr><th>Role</th><th>Email</th><th>Password</th><th>QR Payload</th></tr>
                    </thead>
                    <tbody>
                      <tr><td><strong>Conductor</strong></td><td><code>conductor@catchbus.lk</code></td><td><code>Conductor@123</code></td><td><code>CONDUCTOR-DEMO-QR</code></td></tr>
                      <tr><td><strong>Driver</strong></td><td><code>driver@catchbus.lk</code></td><td><code>Driver@123</code></td><td><code>DRIVER-DEMO-QR</code></td></tr>
                      <tr><td><strong>Administrator</strong></td><td><code>admin@catchbus.lk</code></td><td><code>Admin@123</code></td><td><em>N/A (Email Login)</em></td></tr>
                    </tbody>
                  </table>
                </div>

                <div class="card">
                  <h3>Active REST Endpoints</h3>
                  <table>
                    <thead>
                      <tr><th>Method</th><th>Endpoint</th><th>Purpose</th></tr>
                    </thead>
                    <tbody>
                      <tr><td>GET</td><td><a href="/api/health"><code>/api/health</code></a></td><td>Health check status (JSON)</td></tr>
                      <tr><td>POST</td><td><code>/api/auth/staff/login</code></td><td>Staff QR or credential login (JWT)</td></tr>
                      <tr><td>POST</td><td><code>/api/auth/admin/login</code></td><td>Admin login (JWT)</td></tr>
                      <tr><td>GET</td><td><code>/api/staff/dashboard</code></td><td>Assigned schedule &amp; preceding bus GPS</td></tr>
                      <tr><td>POST</td><td><code>/api/staff/location</code></td><td>Broadcast live bus GPS coordinates</td></tr>
                      <tr><td>POST</td><td><code>/api/conductor/board</code></td><td>Verify passenger QR &amp; mark boarded</td></tr>
                      <tr><td>GET</td><td><code>/api/admin/users</code></td><td>Admin RBAC user management</td></tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </body>
            </html>
            """;
    }
}
