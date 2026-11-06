const formatStars = (n) => (n < 1000 ? String(n) : `${(n / 1000).toFixed(1).replace(/\.0$/, "")}k`);

for (const card of document.querySelectorAll(".github-card[data-repo]")) {
  const repo = card.dataset.repo;
  const key = `github-card:${repo}`;
  const show = (stars) => {
    const count = card.querySelector(".github-card-star-count");
    count.textContent = formatStars(stars);
    count.hidden = false;
  };
  const cached = sessionStorage.getItem(key);
  if (cached !== null) {
    show(Number(cached));
    continue;
  }
  fetch(`https://api.github.com/repos/${repo}`)
    .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
    .then((data) => {
      sessionStorage.setItem(key, String(data.stargazers_count));
      show(data.stargazers_count);
    })
    .catch(() => {});
}
