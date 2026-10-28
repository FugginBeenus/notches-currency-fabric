# Wiki pages

A full copy of the GitHub wiki, kept in the repo so the pages are versioned with the code and
survive the wiki repo being lost. The wiki itself lives at
`git@github.com:FugginBeenus/notches-currency-fabric.wiki.git`.

Edit the pages here, then push them at release time.

## Pushing

1. Clone the wiki repo somewhere outside this one.
2. **Check `git log` on the clone first.** Pages get edited on github.com between releases, so a
   blind copy from this folder can quietly undo those edits.
3. Diff each page you are about to replace. Keep anything that was changed on the wiki and is not
   in the copy here.
4. Copy the pages over, commit, push.
5. Copy the result back into this folder, so the two stay the same.

## Per release

- Bump `**Version:**` on `Home.md` to match `gradle.properties`.
- A new feature needs its own page, an entry in the `Home.md` contents list, and a line in the
  `Home.md` overview.
