# Example External Server

This Node.js server demonstrates how an external controller can interact with
the NFC Emulator app using the same JSON API exposed by the internal server.
It keeps AIDs, scenarios and a communication log in memory and can handle
multiple commands in a single request or concurrently across requests.
Each `POST /` queues a command payload and the app retrieves the next
pending item with `GET /`.

## Setup

```bash
cd example-server
npm install
```

## Running

```bash
npm start
```

The server listens on port `1818` by default. Use the `PORT` environment
variable to change it. Every request is printed to the console for debugging.

## Endpoints

The app polls the server with `GET /` requests. If a queued command is
available, the server responds with that JSON object and removes it from the
queue; otherwise it returns an empty body.

To enqueue commands, send them as `POST` requests with a JSON body to the root
path `/`. The payload structure matches the
[HTTP Control API](../README.md#http-control-api)
of the app. Multiple command groups may be combined in one payload. Example:

```bash
curl -X POST http://localhost:1818/ -H "Content-Type: application/json" \
  -d '{"Aid":{"Add":"A0000002471001"},"Comm":{"Clear":true}}'
```

For a more comprehensive demo, the repository includes
[`multi-command-request.json`](multi-command-request.json) which registers two
AIDs, adds a scenario, sets it as current, adds a communication filter and clears
the log all at once:

```bash
curl -X POST http://localhost:1818/ -H "Content-Type: application/json" \
-d @multi-command-request.json
```

The server also exposes `GET /STATUS` to report the last status value posted by
the app and `POST /STATUS` for the app to send status updates. Two helper
diagnostic endpoints are available as well: `GET /APP-NFC` returns a static
handshake string so you can verify you are querying the emulator server, and
`GET /timestamp` responds with the current Unix timestamp in milliseconds.

## Log management

In the app, the **Logs** button now opens a dedicated screen where you can:

* Configure the log storage limit between `0` and `100` megabytes. When the
  limit is greater than zero, the emulator automatically removes the oldest log
  files inside its internal `logs/` directory before and after saving so the
  total size stays within the configured cap. A value of `0` disables automatic
  cleanup.
* Specify an optional relative path (for example `Weekly3`) that will be created
  beneath the default log directory. Saved files continue to use the scenario
  name plus a timestamp (e.g. `MyScenario_20240101_120000_000.log`).
* Trigger a save directly from the screen. The app stores logs under
  `Android/data/<package>/files/logs/` (or the equivalent internal storage
  directory if external storage is unavailable) and applies both the chosen path
  and storage limit automatically.

Both the path and the storage limit are persisted locally and may be adjusted by
server commands. The `Comm` payload accepts a new `Logs` object in addition to
the existing `Save` field:

```json
{
  "Comm": {
    "Logs": {
      "Path": "Weekly3",
      "MaxStorageMb": 50,
      "Save": true
    }
  }
}
```

`Path` and `MaxStorageMb` update the persisted values. Setting `Save` (or the
existing top-level `Comm.Save`) to `true` writes the current communication log
using those stored preferences. Sending a string to `Comm.Save` still updates the
path and performs an immediate save, and nested objects may include
`Enabled:false` to skip writing while changing settings. The example server logs
each change and echoes the effective storage configuration when a save request
is processed.
