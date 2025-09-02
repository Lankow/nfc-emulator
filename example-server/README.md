# Example External Server

This Node.js server demonstrates how an external controller can interact with
the NFC Emulator app using the same JSON API exposed by the internal server.
It keeps AIDs, scenarios and a communication log in memory and can handle
multiple commands in a single request or concurrently across requests.

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
variable to change it.

## Endpoints

The app polls the server with `GET /` requests, to which the server responds
with an empty body. When commands need to be executed, send them as `POST`
requests with a JSON body to the root path `/`.
The payload structure matches the [HTTP Control API](../README.md#http-control-api)
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
