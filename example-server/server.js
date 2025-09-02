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

// Unified POST endpoint matching the app's internal server API.
app.post('/', async (req, res) => {
  try {
    const { Type } = req.body;
    switch (Type) {
      case 'Aid':
        handleAid(req.body);
        return res.status(HTTP_OK).json({ aids: Array.from(aids) });
      case 'Comm':
        handleComm(req.body);
        return res.status(HTTP_OK).json({ logLength: logEntries.length });
      case 'Scenarios':
        handleScenarios(req.body);
        return res.status(HTTP_OK).json({ scenarios: Array.from(scenarios.keys()) });
      default:
        return res.status(HTTP_INTERNAL_SERVER_ERROR).json({ error: 'Unknown Type' });
    }
  } catch (err) {
    console.error(err);
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

function handleComm({ Clear, Save, Mute, CurrentScenario }) {
  if (Clear) logEntries = [];
  if (Save) console.log('Saving log', logEntries);
  if (typeof Mute === 'boolean') console.log(`Communication ${Mute ? 'muted' : 'unmuted'}`);
  if (CurrentScenario) console.log(`Current scenario: ${CurrentScenario}`);
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

app.listen(PORT, () => {
  console.log(`External server listening on port ${PORT}`);
});
