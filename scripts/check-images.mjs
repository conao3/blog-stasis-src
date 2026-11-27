import { existsSync, readdirSync, readFileSync, statSync } from "node:fs"
import { dirname, join, relative, resolve } from "node:path"

const root = resolve("generated/contents")
const publicDir = resolve("resources/public")

function* markdownFiles(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name)
    if (statSync(path).isDirectory()) yield* markdownFiles(path)
    else if (name.endsWith(".md")) yield path
  }
}

const problems = []
let count = 0
for (const file of markdownFiles(root)) {
  const text = readFileSync(file, "utf8")
  const header = text.split(/\n\s*\n/)[0]
  if (/^Draft:\s*true$/m.test(header)) continue
  const prose = text
    .replace(/^(```|~~~)[^\n]*\n[\s\S]*?^\1[^\n]*$/gm, "")
    .replace(/`[^`\n]*`/g, "")
  for (const [, target] of prose.matchAll(/!\[[^\]]*\]\(\s*<?([^)\s>]+)>?[^)]*\)/g)) {
    count++
    const where = relative(".", file)
    if (/^[a-z][a-z0-9+.-]*:/i.test(target)) {
      problems.push(`${where}: ${target} is not in resources/public`)
      continue
    }
    const path = resolve(dirname(file), decodeURI(target.split(/[?#]/)[0]))
    if (!path.startsWith(publicDir + "/")) problems.push(`${where}: ${target} is not in resources/public`)
    else if (!existsSync(path)) problems.push(`${where}: ${target} does not exist`)
  }
}
for (const problem of problems) console.log(problem)
console.log(`${count} images, ${problems.length} problems`)
process.exit(problems.length ? 1 : 0)
