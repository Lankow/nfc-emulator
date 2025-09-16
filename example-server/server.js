const express = require('express');

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
const logSettings = {
  path: '',
  maxStorageMb: 10,
};

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
          handleAid(data);
          return res.status(HTTP_OK).json({ queued: queue.length, aids: Array.from(aids) });
        case 'Comm':
          handleComm(data);
          return res.status(HTTP_OK).json({ queued: queue.length, logLength: logEntries.length });
        case 'Scenarios':
          handleScenarios(data);
          return res.status(HTTP_OK).json({ queued: queue.length, scenarios: Array.from(scenarios.keys()) });
        default:
          return res.status(HTTP_INTERNAL_SERVER_ERROR).json({ error: 'Unknown Type' });
      }
    }

    if (data.Aid) handleAid(data.Aid);
    if (data.Comm) handleComm(data.Comm);
    if (data.Scenarios) handleScenarios(data.Scenarios);

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

function handleAid({ Add, Remove, Clear }) {
  if (Clear) aids.clear();
  if (Add) {
    const list = Array.isArray(Add) ? Add : [Add];
    list.forEach(aid => aids.add(aid));
  }
  if (Remove) {
    const list = Array.isArray(Remove) ? Remove : [Remove];
    list.forEach(aid => aids.delete(aid));
  }
}

function handleComm({ Clear, Save, Mute, CurrentScenario, Logs }) {
  if (Clear) logEntries = [];
  if (Logs) applyLogSettings(Logs);
  if (Save) handleSaveRequest(Save);
  if (typeof Mute === 'boolean') console.log(`Communication ${Mute ? 'muted' : 'unmuted'}`);
  if (CurrentScenario) console.log(`Current scenario: ${CurrentScenario}`);
}

function applyLogSettings(settings) {
  if (typeof settings !== 'object' || settings === null) return;
  const path = settings.Path ?? settings.path;
  if (typeof path === 'string') {
    logSettings.path = path.trim();
    console.log(`Updated log path to '${logSettings.path || '/'}'`);
  }

  const limitRaw = settings.MaxStorageMb ?? settings.maxStorageMb;
  if (limitRaw !== undefined) {
    const parsed = Number(limitRaw);
    if (!Number.isNaN(parsed)) {
      const limit = Math.max(0, Math.min(100, Math.round(parsed)));
      logSettings.maxStorageMb = limit;
      console.log(`Updated log max storage to ${limit} MB`);
    }
  }

  const save = settings.Save ?? settings.save;
  if (save) {
    handleSaveRequest(save);
  }
}

function handleSaveRequest(save) {
  if (!save) return;
  if (typeof save === 'string') {
    logSettings.path = save.trim();
  } else if (typeof save === 'object') {
    if (typeof save.Path === 'string' || typeof save.path === 'string') {
      logSettings.path = (save.Path ?? save.path).trim();
    }
    const limitRaw = save.MaxStorageMb ?? save.maxStorageMb;
    if (limitRaw !== undefined) {
      const parsed = Number(limitRaw);
      if (!Number.isNaN(parsed)) {
        logSettings.maxStorageMb = Math.max(0, Math.min(100, Math.round(parsed)));
      }
    }
    if (save.Enabled === false || save.enabled === false) {
      console.log('Skip log save (disabled by server command)');
      return;
    }
  }
  console.log('Saving log with settings', {
    path: logSettings.path || '/',
    maxStorageMb: logSettings.maxStorageMb,
    entries: logEntries,
  });
}

function handleScenarios({ Add, Remove, Clear, Current }) {
  if (Clear) scenarios.clear();
  if (Add) {
    const list = Array.isArray(Add) ? Add : [Add];
    list.forEach(sc => scenarios.set(sc.name, sc));
  }
  if (Remove) {
    const list = Array.isArray(Remove) ? Remove : [Remove];
    list.forEach(name => scenarios.delete(name));
  }
  if (Current) console.log(`Set current scenario to ${Current}`);
}

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

app.get('/APP-NFC', (_req, res) => {
  console.log('GET /APP-NFC -> APP-NFC');
  res.status(HTTP_OK).send('APP-NFC');
});

app.get('/timestamp', (_req, res) => {
  const now = Date.now().toString();
  console.log(`GET /timestamp -> ${now}`);
  res.status(HTTP_OK).send(now);
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
