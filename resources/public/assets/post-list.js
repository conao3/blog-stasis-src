for (const list of document.querySelectorAll(".post-list")) {
  const group = list.querySelector(".lang-filter");
  if (!group) continue;
  const buttons = [...group.querySelectorAll("button")];
  const filters = buttons.map((button) => button.dataset.filter);
  const apply = () => {
    const value = new URLSearchParams(location.search).get("lang");
    const filter = filters.includes(value) ? value : "";
    if (filter) list.dataset.filter = filter;
    else delete list.dataset.filter;
    for (const button of buttons) {
      button.setAttribute("aria-pressed", String(button.dataset.filter === filter));
    }
  };
  for (const button of buttons) {
    button.addEventListener("click", () => {
      const url = new URL(location.href);
      if (button.dataset.filter) url.searchParams.set("lang", button.dataset.filter);
      else url.searchParams.delete("lang");
      history.replaceState(null, "", url);
      apply();
    });
  }
  addEventListener("popstate", apply);
  group.hidden = false;
  apply();
}
