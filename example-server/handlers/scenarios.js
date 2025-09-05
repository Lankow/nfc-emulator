function handleScenarios(scenarios, { Add, Remove, Clear, Current }) {
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

module.exports = { handleScenarios };
