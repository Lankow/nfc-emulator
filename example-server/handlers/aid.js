function handleAid(aids, { Add, Remove, Clear }) {
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

module.exports = { handleAid };
