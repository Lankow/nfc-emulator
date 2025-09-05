const express = require('express');
const { handleAid } = require('./handlers/aid');
const { handleComm } = require('./handlers/comm');
const { handleScenarios } = require('./handlers/scenarios');

const HTTP_OK = 200;
const HTTP_INTERNAL_SERVER_ERROR = 500;
const PORT = process.env.PORT || 1818;

const app = express();
app.use(express.json());

// In-memory stores for AIDs, scenarios and log entries.
const aids = new Set();
const scenarios = new Map();
let logEntries = [];
let appStatus = 'IDLE';

// Queue of pending commands for the app to fetch via GET /
const queue = [];

// Unified POST endpoint matching the app's internal server API.
app.post('/', async (req, res) => {
  try {
    const data = req.body;
    console.log('POST / -> queued command:', JSON.stringify(data));
    queue.push(data);

    if (data.Type) {
      switch (data.Type) {
        case 'Aid':
          handleAid(aids, data);
          return res.status(HTTP_OK).json({ queued: queue.length, aids: Array.from(aids) });
        case 'Comm':
          handleComm(logEntries, data);
          return res.status(HTTP_OK).json({ queued: queue.length, logLength: logEntries.length });
        case 'Scenarios':
          handleScenarios(scenarios, data);
          return res.status(HTTP_OK).json({ queued: queue.length, scenarios: Array.from(scenarios.keys()) });
        default:
          return res.status(HTTP_INTERNAL_SERVER_ERROR).json({ error: 'Unknown Type' });
      }
    }

    if (data.Aid) handleAid(aids, data.Aid);
    if (data.Comm) handleComm(logEntries, data.Comm);
    if (data.Scenarios) handleScenarios(scenarios, data.Scenarios);

    return res.status(HTTP_OK).json({
      queued: queue.length,
      aids: Array.from(aids),
      logLength: logEntries.length,
      scenarios: Array.from(scenarios.keys())
    });
  } catch (err) {
    console.error('POST / -> error', err);
    res.status(HTTP_INTERNAL_SERVER_ERROR).json({ error: 'Internal Server Error' });
  }
});

// Polling endpoint used by the app's ServerConnectionManager.
app.get('/', (_req, res) => {
  if (queue.length === 0) {
    console.log('GET / -> no queued commands');
    return res.status(HTTP_OK).end();
  }
  const next = queue.shift();
  console.log('GET / -> dispatching command:', JSON.stringify(next));
  res.status(HTTP_OK).json(next);
});

// Endpoint for the app to clear any queued commands once handled.
app.delete('/', (_req, res) => {
  queue.length = 0;
  console.log('DELETE / -> cleared queued commands');
  res.status(HTTP_OK).end();
});

// Endpoint for reporting and querying app status.
app.get('/STATUS', (_req, res) => {
  res.status(HTTP_OK).json({ status: appStatus });
});

app.post('/STATUS', (req, res) => {
  const { status } = req.body || {};
  if (typeof status === 'string') {
    appStatus = status;
    console.log('POST /STATUS ->', status);
    res.status(HTTP_OK).end();
  } else {
    res.status(HTTP_INTERNAL_SERVER_ERROR).json({ error: 'Missing status' });
  }
});

app.listen(PORT, () => {
  console.log(`External server listening on port ${PORT}`);
});
