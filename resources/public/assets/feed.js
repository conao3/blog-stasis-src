for (const box of document.querySelectorAll(".feed-copy")) {
  const input = box.querySelector("input");
  const button = box.querySelector("button");
  input.addEventListener("focus", () => input.select());
  if (!navigator.clipboard) continue;
  const label = button.textContent;
  button.hidden = false;
  button.addEventListener("click", () => {
    navigator.clipboard.writeText(input.value).then(
      () => {
        button.textContent = "コピーしました";
        setTimeout(() => {
          button.textContent = label;
        }, 2000);
      },
      () => input.select(),
    );
  });
}
