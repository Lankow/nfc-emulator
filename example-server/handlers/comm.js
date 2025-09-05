function handleComm(logEntries, { Clear, Save, Mute, CurrentScenario }) {
  if (Clear) logEntries.length = 0;
  if (Save) console.log('Saving log', logEntries);
  if (typeof Mute === 'boolean') console.log(`Communication ${Mute ? 'muted' : 'unmuted'}`);
  if (CurrentScenario) console.log(`Current scenario: ${CurrentScenario}`);
}

module.exports = { handleComm };
