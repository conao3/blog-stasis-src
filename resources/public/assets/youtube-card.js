for (const card of document.querySelectorAll(".youtube-card[data-id]")) {
  card.querySelector("a").addEventListener("click", (event) => {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.button !== 0) return;
    event.preventDefault();
    const title = card.querySelector(".youtube-card-title")?.textContent ?? "YouTube";
    const iframe = document.createElement("iframe");
    iframe.className = "youtube-card-player";
    iframe.src = `https://www.youtube-nocookie.com/embed/${card.dataset.id}?autoplay=1`;
    iframe.title = title;
    iframe.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share";
    iframe.referrerPolicy = "strict-origin-when-cross-origin";
    iframe.allowFullscreen = true;
    card.replaceChildren(iframe);
  });
}
