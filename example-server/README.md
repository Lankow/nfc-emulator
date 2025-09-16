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
