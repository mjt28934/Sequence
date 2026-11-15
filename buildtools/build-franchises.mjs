/**
 * Adds franchises to app/src/main/assets/sequence.json.
 *
 * Each film/series below carries the Wikipedia article used to look its release date up on Wikidata
 * (earliest day-precision P577). The `date` field is only a fallback for when the lookup fails, so the
 * watch orders come from real data rather than from memory.
 *
 * Idempotent: running it again replaces the franchises it owns and leaves the rest alone.
 *
 *   node build-franchises.mjs            # everything
 *   node build-franchises.mjs bond rocky # a subset (dates are cached in .date-cache.json)
 */
import fs from "node:fs";
import path from "node:path";

const root = "C:/Projects/Orderly/buildtools";
const catalogPath = "C:/Projects/Orderly/app/src/main/assets/sequence.json";
const cachePath = path.join(root, ".date-cache.json");
const UA = { "User-Agent": "Sequence/1.6 (offline watch-order app; date lookup)" };

/* ---------------------------------------------------------------- franchise data ---- */

const NEW_FRANCHISES = [
  {
    id: "bond",
    name: "James Bond",
    genre: "Action",
    accent: "#c9a227",
    hook: "Twenty-five Eon films, one man, loose continuity",
    blurb:
      "The Eon series ran from 1962 to 2021 and rebooted itself twice, so there is no single story order. Watch it in release order and you get every era, every Bond and every gadget in the order audiences met them.",
    storyHint: "Release order is the honest answer. The two non-Eon films are optional and sit outside the series.",
    eraOrder: ["Eon", "Other"],
    entries: [
      { id: "bd-dn", title: "Dr. No", page: "Dr. No (film)", date: "1962-10-05", era: "Eon" },
      { id: "bd-fr", title: "From Russia with Love", page: "From Russia with Love (film)", date: "1963-10-10", era: "Eon" },
      { id: "bd-gf", title: "Goldfinger", page: "Goldfinger (film)", date: "1964-09-17", era: "Eon" },
      { id: "bd-tb", title: "Thunderball", page: "Thunderball (film)", date: "1965-12-29", era: "Eon" },
      { id: "bd-yl", title: "You Only Live Twice", page: "You Only Live Twice (film)", date: "1967-06-12", era: "Eon" },
      { id: "bd-oh", title: "On Her Majesty's Secret Service", page: "On Her Majesty's Secret Service (film)", date: "1969-12-18", era: "Eon" },
      { id: "bd-da", title: "Diamonds Are Forever", page: "Diamonds Are Forever (film)", date: "1971-12-17", era: "Eon" },
      { id: "bd-ll", title: "Live and Let Die", page: "Live and Let Die (film)", date: "1973-06-27", era: "Eon" },
      { id: "bd-mg", title: "The Man with the Golden Gun", page: "The Man with the Golden Gun (film)", date: "1974-12-19", era: "Eon" },
      { id: "bd-sm", title: "The Spy Who Loved Me", page: "The Spy Who Loved Me (film)", date: "1977-07-07", era: "Eon" },
      { id: "bd-mr", title: "Moonraker", page: "Moonraker (film)", date: "1979-06-26", era: "Eon" },
      { id: "bd-fy", title: "For Your Eyes Only", page: "For Your Eyes Only (film)", date: "1981-06-24", era: "Eon" },
      { id: "bd-oc", title: "Octopussy", page: "Octopussy", date: "1983-06-06", era: "Eon" },
      { id: "bd-ns", title: "Never Say Never Again", page: "Never Say Never Again", date: "1983-10-07", era: "Other", optional: true, note: "A remake of Thunderball made outside Eon, with Sean Connery back as Bond." },
      { id: "bd-vk", title: "A View to a Kill", page: "A View to a Kill", date: "1985-05-22", era: "Eon" },
      { id: "bd-ld", title: "The Living Daylights", page: "The Living Daylights", date: "1987-06-29", era: "Eon" },
      { id: "bd-lk", title: "Licence to Kill", page: "Licence to Kill", date: "1989-06-13", era: "Eon" },
      { id: "bd-ge", title: "GoldenEye", page: "GoldenEye", date: "1995-11-17", era: "Eon" },
      { id: "bd-tn", title: "Tomorrow Never Dies", page: "Tomorrow Never Dies", date: "1997-12-09", era: "Eon" },
      { id: "bd-we", title: "The World Is Not Enough", page: "The World Is Not Enough", date: "1999-11-19", era: "Eon" },
      { id: "bd-dad", title: "Die Another Day", page: "Die Another Day", date: "2002-11-20", era: "Eon" },
      { id: "bd-cr", title: "Casino Royale", page: "Casino Royale (2006 film)", date: "2006-11-16", era: "Eon", note: "The first Craig film and a hard reboot of the series." },
      { id: "bd-qs", title: "Quantum of Solace", page: "Quantum of Solace", date: "2008-10-31", era: "Eon", note: "Picks up minutes after Casino Royale ends." },
      { id: "bd-sf", title: "Skyfall", page: "Skyfall", date: "2012-10-26", era: "Eon" },
      { id: "bd-sp", title: "Spectre", page: "Spectre (2015 film)", date: "2015-10-26", era: "Eon" },
      { id: "bd-nt", title: "No Time to Die", page: "No Time to Die", date: "2021-09-30", era: "Eon" },
      { id: "bd-cr67", title: "Casino Royale", page: "Casino Royale (1967 film)", date: "1967-04-13", era: "Other", optional: true, note: "A 1967 comedy parody, unrelated to the Eon series." },
    ],
    works: [
      { id: "bk-bd-1", title: "Casino Royale", year: 1953, format: "novel", by: "Ian Fleming", page: "Casino Royale (novel)", path: "Fleming" },
      { id: "bk-bd-2", title: "Live and Let Die", year: 1954, format: "novel", by: "Ian Fleming", page: "Live and Let Die (novel)", path: "Fleming" },
      { id: "bk-bd-3", title: "Moonraker", year: 1955, format: "novel", by: "Ian Fleming", page: "Moonraker (novel)", path: "Fleming" },
      { id: "bk-bd-4", title: "Diamonds Are Forever", year: 1956, format: "novel", by: "Ian Fleming", page: "Diamonds Are Forever (novel)", path: "Fleming" },
      { id: "bk-bd-5", title: "From Russia, with Love", year: 1957, format: "novel", by: "Ian Fleming", page: "From Russia, with Love (novel)", path: "Fleming" },
      { id: "bk-bd-6", title: "Dr. No", year: 1958, format: "novel", by: "Ian Fleming", page: "Dr. No (novel)", path: "Fleming" },
      { id: "bk-bd-7", title: "Goldfinger", year: 1959, format: "novel", by: "Ian Fleming", page: "Goldfinger (novel)", path: "Fleming" },
      { id: "bk-bd-8", title: "Thunderball", year: 1961, format: "novel", by: "Ian Fleming", page: "Thunderball (novel)", path: "Fleming" },
      { id: "bk-bd-9", title: "The Spy Who Loved Me", year: 1962, format: "novel", by: "Ian Fleming", page: "The Spy Who Loved Me (novel)", path: "Fleming" },
      { id: "bk-bd-10", title: "On Her Majesty's Secret Service", year: 1963, format: "novel", by: "Ian Fleming", page: "On Her Majesty's Secret Service (novel)", path: "Fleming" },
      { id: "bk-bd-11", title: "You Only Live Twice", year: 1964, format: "novel", by: "Ian Fleming", page: "You Only Live Twice (novel)", path: "Fleming" },
      { id: "bk-bd-12", title: "The Man with the Golden Gun", year: 1965, format: "novel", by: "Ian Fleming", page: "The Man with the Golden Gun (novel)", path: "Fleming" },
      { id: "bk-bd-13", title: "Octopussy and The Living Daylights", year: 1966, format: "short-stories", by: "Ian Fleming", page: "Octopussy and The Living Daylights", path: "Fleming" },
    ],
  },

  {
    id: "mission-impossible",
    name: "Mission: Impossible",
    genre: "Action",
    accent: "#d0463f",
    hook: "Eight films, one increasingly impossible stunt",
    blurb:
      "The film series rebooted the 1960s show with Ethan Hunt and has kept a single loose continuity ever since. Each film hands the director a new style, so release order is the only order that makes sense.",
    storyHint: "Release order. Dead Reckoning and The Final Reckoning are one story split in two.",
    eraOrder: ["Films"],
    entries: [
      { id: "mi-1", title: "Mission: Impossible", page: "Mission: Impossible (film)", date: "1996-05-22", era: "Films" },
      { id: "mi-2", title: "Mission: Impossible 2", page: "Mission: Impossible 2", date: "2000-05-24", era: "Films" },
      { id: "mi-3", title: "Mission: Impossible III", page: "Mission: Impossible III", date: "2006-05-05", era: "Films" },
      { id: "mi-4", title: "Mission: Impossible â€“ Ghost Protocol", page: "Mission: Impossible â€“ Ghost Protocol", date: "2011-12-21", era: "Films" },
      { id: "mi-5", title: "Mission: Impossible â€“ Rogue Nation", page: "Mission: Impossible â€“ Rogue Nation", date: "2015-07-31", era: "Films" },
      { id: "mi-6", title: "Mission: Impossible â€“ Fallout", page: "Mission: Impossible â€“ Fallout", date: "2018-07-27", era: "Films", note: "Direct follow-on from Rogue Nation." },
      { id: "mi-7", title: "Mission: Impossible â€“ Dead Reckoning Part One", page: "Mission: Impossible â€“ Dead Reckoning Part One", date: "2023-07-12", era: "Films" },
      { id: "mi-8", title: "Mission: Impossible â€“ The Final Reckoning", page: "Mission: Impossible â€“ The Final Reckoning", date: "2025-05-23", era: "Films", note: "Second half of the Dead Reckoning story." },
    ],
    works: [],
  },

  {
    id: "john-wick",
    name: "John Wick",
    genre: "Action",
    accent: "#8b7cf0",
    hook: "A hitman, a dog, and a very expensive suit",
    blurb:
      "Four films, one prequel series and one spin-off, all built on the same hotel-for-assassins mythology. Story order puts the 1970s Continental series first, then the films, with Ballerina tucked between Chapters 3 and 4.",
    storyHint: "The Continental is a 1970s prequel; Ballerina runs alongside Chapter 3 and ends before Chapter 4.",
    eraOrder: ["Prequel", "Main"],
    entries: [
      { id: "jw-cont", title: "The Continental", page: "The Continental: From the World of John Wick", date: "2023-09-22", kind: "series", era: "Prequel", optional: true, note: "A three-part prequel set in 1975. Optional, but it explains the hotel." },
      { id: "jw-1", title: "John Wick", page: "John Wick (film)", date: "2014-10-24", era: "Main" },
      { id: "jw-2", title: "John Wick: Chapter 2", page: "John Wick: Chapter 2", date: "2017-02-10", era: "Main" },
      { id: "jw-3", title: "John Wick: Chapter 3 â€“ Parabellum", page: "John Wick: Chapter 3 â€“ Parabellum", date: "2019-05-17", era: "Main" },
      { id: "jw-bal", title: "Ballerina", page: "Ballerina (2025 film)", date: "2025-06-06", era: "Main", note: "Set during the events of Chapter 3." },
      { id: "jw-4", title: "John Wick: Chapter 4", page: "John Wick: Chapter 4", date: "2023-03-24", era: "Main" },
    ],
    works: [],
  },

  {
    id: "matrix",
    name: "The Matrix",
    genre: "Sci-fi",
    accent: "#35c46a",
    hook: "Follow the white rabbit, then the sequels",
    blurb:
      "The 1999 original, two sequels released six months apart, an animated anthology that fills in the world, and a 2021 epilogue. The Animatrix sits between the first two films.",
    storyHint: "The Matrix, The Animatrix, Reloaded, Revolutions, then Resurrections twenty years later.",
    eraOrder: ["Films"],
    entries: [
      { id: "mx-1", title: "The Matrix", page: "The Matrix", date: "1999-03-31", era: "Films" },
      { id: "mx-an", title: "The Animatrix", page: "The Animatrix", date: "2003-06-03", kind: "special", era: "Films", optional: true, note: "Nine animated shorts that set up the world and the war. Watch after the first film." },
      { id: "mx-2", title: "The Matrix Reloaded", page: "The Matrix Reloaded", date: "2003-05-15", era: "Films" },
      { id: "mx-3", title: "The Matrix Revolutions", page: "The Matrix Revolutions", date: "2003-11-05", era: "Films" },
      { id: "mx-4", title: "The Matrix Resurrections", page: "The Matrix Resurrections", date: "2021-12-22", era: "Films" },
    ],
    works: [],
  },

  {
    id: "back-to-the-future",
    name: "Back to the Future",
    genre: "Sci-fi",
    accent: "#e8b23c",
    hook: "Three films, one DeLorean, no reboots",
    blurb:
      "A single tight trilogy about the same afternoon in 1985 going wrong in three directions, plus an animated series that continues after Part III. Release order is story order.",
    storyHint: "Parts I, II and III in order. The animated series is optional and runs after the trilogy.",
    eraOrder: ["Films", "Animated"],
    entries: [
      { id: "bttf-1", title: "Back to the Future", page: "Back to the Future", date: "1985-07-03", era: "Films" },
      { id: "bttf-2", title: "Back to the Future Part II", page: "Back to the Future Part II", date: "1989-11-22", era: "Films" },
      { id: "bttf-3", title: "Back to the Future Part III", page: "Back to the Future Part III", date: "1990-05-25", era: "Films" },
      { id: "bttf-as", title: "Back to the Future: The Animated Series", page: "Back to the Future: The Animated Series", date: "1991-09-14", kind: "series", era: "Animated", optional: true },
    ],
    works: [],
  },

  {
    id: "pirates",
    name: "Pirates of the Caribbean",
    genre: "Fantasy",
    accent: "#2fb1a8",
    hook: "Five films, one curse, a lot of rum",
    blurb:
      "The first three films are one continuous story; the last two are stand-alone adventures with the same crew. Release order keeps the trilogy intact.",
    storyHint: "Release order. Dead Men Tell No Tales brings back characters from the trilogy.",
    eraOrder: ["Films"],
    entries: [
      { id: "potc-1", title: "Pirates of the Caribbean: The Curse of the Black Pearl", page: "Pirates of the Caribbean: The Curse of the Black Pearl", date: "2003-07-09" },
      { id: "potc-2", title: "Pirates of the Caribbean: Dead Man's Chest", page: "Pirates of the Caribbean: Dead Man's Chest", date: "2006-07-07" },
      { id: "potc-3", title: "Pirates of the Caribbean: At World's End", page: "Pirates of the Caribbean: At World's End", date: "2007-05-25" },
      { id: "potc-4", title: "Pirates of the Caribbean: On Stranger Tides", page: "Pirates of the Caribbean: On Stranger Tides", date: "2011-05-20" },
      { id: "potc-5", title: "Pirates of the Caribbean: Dead Men Tell No Tales", page: "Pirates of the Caribbean: Dead Men Tell No Tales", date: "2017-05-26" },
    ],
    works: [],
  },

  {
    id: "planet-of-the-apes",
    name: "Planet of the Apes",
    genre: "Sci-fi",
    accent: "#8c9a6b",
    hook: "Three separate timelines about the same planet",
    blurb:
      "The 1968 original and its four sequels, Tim Burton's 2001 remake, and the modern Caesar trilogy that leads into Kingdom. They are three different continuities, so pick one lane and stay in it.",
    storyHint:
      "Caesar timeline: Rise, Dawn, War, Kingdom. Original timeline: Planet, Beneath, Escape, Conquest, Battle. The 2001 remake stands alone.",
    eraOrder: ["Original", "Remake", "Caesar"],
    entries: [
      { id: "pota-1", title: "Planet of the Apes", page: "Planet of the Apes (1968 film)", date: "1968-04-03", era: "Original" },
      { id: "pota-2", title: "Beneath the Planet of the Apes", page: "Beneath the Planet of the Apes", date: "1970-05-26", era: "Original" },
      { id: "pota-3", title: "Escape from the Planet of the Apes", page: "Escape from the Planet of the Apes", date: "1971-05-21", era: "Original" },
      { id: "pota-4", title: "Conquest of the Planet of the Apes", page: "Conquest of the Planet of the Apes", date: "1972-06-30", era: "Original" },
      { id: "pota-5", title: "Battle for the Planet of the Apes", page: "Battle for the Planet of the Apes", date: "1973-06-15", era: "Original" },
      { id: "pota-01", title: "Planet of the Apes", page: "Planet of the Apes (2001 film)", date: "2001-07-27", era: "Remake", optional: true, note: "Tim Burton's remake. Its own continuity, safe to skip." },
      { id: "pota-r", title: "Rise of the Planet of the Apes", page: "Rise of the Planet of the Apes", date: "2011-08-05", era: "Caesar" },
      { id: "pota-d", title: "Dawn of the Planet of the Apes", page: "Dawn of the Planet of the Apes", date: "2014-07-11", era: "Caesar" },
      { id: "pota-w", title: "War for the Planet of the Apes", page: "War for the Planet of the Apes", date: "2017-07-14", era: "Caesar" },
      { id: "pota-k", title: "Kingdom of the Planet of the Apes", page: "Kingdom of the Planet of the Apes", date: "2024-05-10", era: "Caesar", note: "Set generations after War." },
    ],
    works: [
      { id: "bk-pota-1", title: "Planet of the Apes", year: 1963, format: "novel", by: "Pierre Boulle", page: "Planet of the Apes (novel)", path: "Novels" },
    ],
  },

  {
    id: "rocky",
    name: "Rocky & Creed",
    genre: "Action",
    accent: "#b6562d",
    hook: "Nine films, one staircase",
    blurb:
      "Rocky ran from 1976 to 2006, then handed the gloves to Adonis Creed in 2015. It is one continuous story, so release order is story order.",
    storyHint: "Rocky Iâ€“V, Rocky Balboa, then Creed, Creed II and Creed III.",
    eraOrder: ["Rocky", "Creed"],
    entries: [
      { id: "rk-1", title: "Rocky", page: "Rocky", date: "1976-11-21", era: "Rocky" },
      { id: "rk-2", title: "Rocky II", page: "Rocky II", date: "1979-06-15", era: "Rocky" },
      { id: "rk-3", title: "Rocky III", page: "Rocky III", date: "1982-05-28", era: "Rocky" },
      { id: "rk-4", title: "Rocky IV", page: "Rocky IV", date: "1985-11-27", era: "Rocky" },
      { id: "rk-5", title: "Rocky V", page: "Rocky V", date: "1990-11-16", era: "Rocky" },
      { id: "rk-6", title: "Rocky Balboa", page: "Rocky Balboa (film)", date: "2006-12-20", era: "Rocky", note: "Ignores most of Rocky V and closes the original run." },
      { id: "cr-1", title: "Creed", page: "Creed (film)", date: "2015-11-25", era: "Creed" },
      { id: "cr-2", title: "Creed II", page: "Creed II", date: "2018-11-21", era: "Creed", note: "Brings back Ivan Drago from Rocky IV." },
      { id: "cr-3", title: "Creed III", page: "Creed III", date: "2023-03-03", era: "Creed", note: "The first film without Rocky." },
    ],
    works: [],
  },

  {
    id: "die-hard",
    name: "Die Hard",
    genre: "Action",
    accent: "#5b7fae",
    hook: "One man, five very bad days",
    blurb:
      "The 1988 original invented a genre and the sequels keep finding new buildings for John McClane to wreck. Each film stands alone; release order is enough.",
    storyHint: "Release order. Only the first two are closely linked.",
    eraOrder: ["Films"],
    entries: [
      { id: "dh-1", title: "Die Hard", page: "Die Hard", date: "1988-07-15" },
      { id: "dh-2", title: "Die Hard 2", page: "Die Hard 2", date: "1990-07-04" },
      { id: "dh-3", title: "Die Hard with a Vengeance", page: "Die Hard with a Vengeance", date: "1995-05-19" },
      { id: "dh-4", title: "Live Free or Die Hard", page: "Live Free or Die Hard", date: "2007-06-27" },
      { id: "dh-5", title: "A Good Day to Die Hard", page: "A Good Day to Die Hard", date: "2013-02-14" },
    ],
    works: [],
  },

  {
    id: "ghostbusters",
    name: "Ghostbusters",
    genre: "Horror",
    accent: "#e0562d",
    hook: "Two timelines, four proton packs",
    blurb:
      "The 1984 original and its 1989 sequel, a 2016 reboot that ignores both, then Afterlife and Frozen Empire which return to the original story. Watch the original two first or Afterlife will not land.",
    storyHint: "Ghostbusters, Ghostbusters II, Afterlife, Frozen Empire. The 2016 film is a separate reboot.",
    eraOrder: ["Original", "Reboot", "Afterlife"],
    entries: [
      { id: "gb-1", title: "Ghostbusters", page: "Ghostbusters", date: "1984-06-08", era: "Original" },
      { id: "gb-2", title: "Ghostbusters II", page: "Ghostbusters II", date: "1989-06-16", era: "Original" },
      { id: "gb-16", title: "Ghostbusters", page: "Ghostbusters (2016 film)", date: "2016-07-15", era: "Reboot", optional: true, note: "A standalone reboot with a new cast. Nothing later depends on it." },
      { id: "gb-al", title: "Ghostbusters: Afterlife", page: "Ghostbusters: Afterlife", date: "2021-11-19", era: "Afterlife" },
      { id: "gb-fe", title: "Ghostbusters: Frozen Empire", page: "Ghostbusters: Frozen Empire", date: "2024-03-22", era: "Afterlife" },
    ],
    works: [],
  },

  {
    id: "men-in-black",
    name: "Men in Black",
    genre: "Sci-fi",
    accent: "#7c8b99",
    hook: "Four films, one neuralyzer",
    blurb:
      "A comic-book adaptation that turned into a trilogy about agents K and J, plus a 2019 spin-off with a different pair. Light, episodic and easy to watch in release order.",
    storyHint: "Release order. Men in Black 3 involves time travel but is seen after the second film.",
    eraOrder: ["Films"],
    entries: [
      { id: "mib-1", title: "Men in Black", page: "Men in Black (1997 film)", date: "1997-07-02" },
      { id: "mib-2", title: "Men in Black II", page: "Men in Black II", date: "2002-07-03" },
      { id: "mib-3", title: "Men in Black 3", page: "Men in Black 3", date: "2012-05-25" },
      { id: "mib-4", title: "Men in Black: International", page: "Men in Black: International", date: "2019-06-14", optional: true, note: "A spin-off with new leads, set in the same world." },
    ],
    works: [],
  },

  {
    id: "toy-story",
    name: "Toy Story",
    genre: "Fantasy",
    accent: "#f2a03d",
    hook: "Five films, one toy chest",
    blurb:
      "Pixar's first film and still its most reliable series. Each entry stands alone but the endings build on each other, so release order gets the emotional payoff.",
    storyHint: "Release order, all five films.",
    eraOrder: ["Films"],
    entries: [
      { id: "ts-1", title: "Toy Story", page: "Toy Story", date: "1995-11-22" },
      { id: "ts-2", title: "Toy Story 2", page: "Toy Story 2", date: "1999-11-24" },
      { id: "ts-3", title: "Toy Story 3", page: "Toy Story 3", date: "2010-06-18" },
      { id: "ts-4", title: "Toy Story 4", page: "Toy Story 4", date: "2019-06-21" },
      { id: "ts-5", title: "Toy Story 5", page: "Toy Story 5", date: "2026-06-19" },
    ],
    works: [],
  },

  {
    id: "shrek",
    name: "Shrek",
    genre: "Fantasy",
    accent: "#7cb342",
    hook: "Four films and two Puss in Boots side stories",
    blurb:
      "DreamWorks' fairy-tale spoof, plus the Puss in Boots spin-offs. The first Puss film is a prequel; The Last Wish is set after the main series.",
    storyHint: "Shrek, Shrek 2, Shrek the Third, Shrek Forever After, then the two Puss in Boots films.",
    eraOrder: ["Shrek", "Spin-offs"],
    entries: [
      { id: "sh-1", title: "Shrek", page: "Shrek", date: "2001-05-18", era: "Shrek" },
      { id: "sh-2", title: "Shrek 2", page: "Shrek 2", date: "2004-05-19", era: "Shrek" },
      { id: "sh-3", title: "Shrek the Third", page: "Shrek the Third", date: "2007-05-18", era: "Shrek" },
      { id: "sh-4", title: "Shrek Forever After", page: "Shrek Forever After", date: "2010-05-21", era: "Shrek" },
      { id: "sh-p1", title: "Puss in Boots", page: "Puss in Boots (2011 film)", date: "2011-10-28", era: "Spin-offs", note: "A prequel, so it can also be watched first." },
      { id: "sh-p2", title: "Puss in Boots: The Last Wish", page: "Puss in Boots: The Last Wish", date: "2022-12-21", era: "Spin-offs" },
    ],
    works: [
      { id: "bk-sh-1", title: "Shrek!", year: 1990, format: "novel", by: "William Steig", page: "Shrek!", path: "Source books" },
    ],
  },

  {
    id: "sonic",
    name: "Sonic the Hedgehog",
    genre: "Action",
    accent: "#3d6df0",
    hook: "Three films and a Knuckles series",
    blurb:
      "The film series that started as a joke and became a solid family action trilogy. Knuckles sits between the second and third films and explains where he went.",
    storyHint: "Sonic, Sonic 2, Knuckles, then Sonic 3.",
    eraOrder: ["Films"],
    entries: [
      { id: "sn-1", title: "Sonic the Hedgehog", page: "Sonic the Hedgehog (film)", date: "2020-02-14", era: "Films" },
      { id: "sn-2", title: "Sonic the Hedgehog 2", page: "Sonic the Hedgehog 2 (film)", date: "2022-04-08", era: "Films" },
      { id: "sn-k", title: "Knuckles", page: "Knuckles (TV series)", date: "2024-04-26", kind: "series", era: "Films", note: "Six episodes set between Sonic 2 and Sonic 3." },
      { id: "sn-3", title: "Sonic the Hedgehog 3", page: "Sonic the Hedgehog 3 (film)", date: "2024-12-20", era: "Films" },
    ],
    works: [],
  },

  {
    id: "avatar",
    name: "Avatar",
    genre: "Sci-fi",
    accent: "#21b6c9",
    hook: "One story, told in four instalments",
    blurb:
      "James Cameron's Pandora films are one continuous story with years between chapters. The 2009 original, The Way of Water, then Fire and Ash.",
    storyHint: "Release order. Each film picks up shortly after the last.",
    eraOrder: ["Films"],
    entries: [
      { id: "av-1", title: "Avatar", page: "Avatar (2009 film)", date: "2009-12-18" },
      { id: "av-2", title: "Avatar: The Way of Water", page: "Avatar: The Way of Water", date: "2022-12-16" },
      { id: "av-3", title: "Avatar: Fire and Ash", page: "Avatar: Fire and Ash", date: "2025-12-19" },
    ],
    works: [],
  },

  {
    id: "twilight",
    name: "The Twilight Saga",
    genre: "Fantasy",
    accent: "#8e6fa8",
    hook: "Five films, one love triangle",
    blurb:
      "Four novels adapted into five films, with the last book split in two. Straight release order, and the books are a genuinely different experience from the films.",
    storyHint: "Twilight, New Moon, Eclipse, Breaking Dawn Parts 1 and 2.",
    eraOrder: ["Films"],
    entries: [
      { id: "tw-1", title: "Twilight", page: "Twilight (2008 film)", date: "2008-11-21" },
      { id: "tw-2", title: "The Twilight Saga: New Moon", page: "The Twilight Saga: New Moon", date: "2009-11-20" },
      { id: "tw-3", title: "The Twilight Saga: Eclipse", page: "The Twilight Saga: Eclipse", date: "2010-06-30" },
      { id: "tw-4", title: "The Twilight Saga: Breaking Dawn â€“ Part 1", page: "The Twilight Saga: Breaking Dawn â€“ Part 1", date: "2011-11-18" },
      { id: "tw-5", title: "The Twilight Saga: Breaking Dawn â€“ Part 2", page: "The Twilight Saga: Breaking Dawn â€“ Part 2", date: "2012-11-16" },
    ],
    works: [
      { id: "bk-tw-1", title: "Twilight", year: 2005, format: "novel", by: "Stephenie Meyer", page: "Twilight (Meyer novel)", path: "The Saga" },
      { id: "bk-tw-2", title: "New Moon", year: 2006, format: "novel", by: "Stephenie Meyer", page: "New Moon (novel)", path: "The Saga" },
      { id: "bk-tw-3", title: "Eclipse", year: 2007, format: "novel", by: "Stephenie Meyer", page: "Eclipse (Meyer novel)", path: "The Saga" },
      { id: "bk-tw-4", title: "Breaking Dawn", year: 2008, format: "novel", by: "Stephenie Meyer", page: "Breaking Dawn", path: "The Saga" },
    ],
  },

  {
    id: "bourne",
    name: "Jason Bourne",
    genre: "Action",
    accent: "#9aa7b2",
    hook: "Five films, one lost identity",
    blurb:
      "The Ludlum novels became a trilogy about amnesia and surveillance, then a spin-off and a reunion. Legacy runs alongside Ultimatum rather than after it.",
    storyHint: "Identity, Supremacy, Ultimatum, Jason Bourne. Legacy happens at the same time as Ultimatum.",
    eraOrder: ["Films"],
    entries: [
      { id: "bn-1", title: "The Bourne Identity", page: "The Bourne Identity (2002 film)", date: "2002-06-14" },
      { id: "bn-2", title: "The Bourne Supremacy", page: "The Bourne Supremacy (film)", date: "2004-07-23" },
      { id: "bn-3", title: "The Bourne Ultimatum", page: "The Bourne Ultimatum (film)", date: "2007-08-03" },
      { id: "bn-l", title: "The Bourne Legacy", page: "The Bourne Legacy (film)", date: "2012-08-10", note: "Set during Ultimatum with a new lead." },
      { id: "bn-jb", title: "Jason Bourne", page: "Jason Bourne (film)", date: "2016-07-29" },
    ],
    works: [
      { id: "bk-bn-1", title: "The Bourne Identity", year: 1980, format: "novel", by: "Robert Ludlum", page: "The Bourne Identity (novel)", path: "Novels" },
      { id: "bk-bn-2", title: "The Bourne Supremacy", year: 1986, format: "novel", by: "Robert Ludlum", page: "The Bourne Supremacy", path: "Novels" },
      { id: "bk-bn-3", title: "The Bourne Ultimatum", year: 1990, format: "novel", by: "Robert Ludlum", page: "The Bourne Ultimatum", path: "Novels" },
    ],
  },
];

/* ------------------------------------------------------------------------- dates ---- */

const cache = fs.existsSync(cachePath)
  ? JSON.parse(fs.readFileSync(cachePath, "utf8").replace(/^\uFEFF/, ""))
  : {};

/**
 * Dates where Wikidata's earliest P577 is a premiere, a festival screening or a date that was later
 * moved, and the actual release is known and different.
 */
const DATE_FIXES = {
  "bd-nt": "2021-09-30", // No Time to Die: Wikidata lists the original 2019 date first
  "av-3": "2025-12-19", // Avatar: Fire and Ash: premiere vs wide release
  "ts-5": "2026-06-19", // Toy Story 5: premiere vs wide release
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const norm = (t) => t.replace(/_/g, " ").trim().toLowerCase();

function earliestDay(claims) {
  const days = (claims ?? [])
    .map((c) => c.mainsnak?.datavalue?.value)
    .filter((v) => v && (v.precision === 11 || v.precision === 10))
    .map((v) => v.time.slice(1, 11))
    .filter((d) => !d.startsWith("0000"))
    .sort();
  return days[0] ?? null;
}

/** Resolves release dates for many articles at once; Wikidata allows several titles per call. */
async function resolveDates(pages) {
  const todo = [...new Set(pages)].filter((p) => !cache[p]);
  for (let i = 0; i < todo.length; i += 20) {
    const chunk = todo.slice(i, i + 20);
    const url =
      "https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&formatversion=2" +
      "&props=claims%7Csitelinks&sites=enwiki&titles=" +
      encodeURIComponent(chunk.join("|"));

    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        const res = await fetch(url, { headers: UA });
        if (res.status === 429) {
          await sleep(3000 * (attempt + 1));
          continue;
        }
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        const entities = json.entities;
        const list = Array.isArray(entities) ? entities : Object.values(entities ?? {});
        const found = new Map();
        for (const e of list) {
          const title = e?.sitelinks?.enwiki?.title;
          const day = earliestDay(e?.claims?.P577);
          if (title && day) found.set(norm(title), day);
        }
        let hit = 0;
        for (const page of chunk) {
          const day = found.get(norm(page));
          if (day) {
            cache[page] = day;
            hit++;
          }
        }
        console.log(`  dates: ${hit}/${chunk.length} resolved`);
        break;
      } catch (err) {
        if (attempt === 2) console.warn(`  ! batch ${i / 20}: ${err.message}`);
        else await sleep(1500);
      }
    }
    await sleep(1200);
  }
}

/* ------------------------------------------------------------------------- build ---- */

function accentTokens(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = ((n >> 16) & 255) / 255;
  const g = ((n >> 8) & 255) / 255;
  const b = (n & 255) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  let h = 0;
  let s = 0;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
    else if (max === g) h = ((b - r) / d + 2) / 6;
    else h = ((r - g) / d + 4) / 6;
  }
  const hsl = (hh, ss, ll) => {
    const q = ll < 0.5 ? ll * (1 + ss) : ll + ss - ll * ss;
    const p = 2 * ll - q;
    const channel = (t) => {
      let tt = t;
      if (tt < 0) tt += 1;
      if (tt > 1) tt -= 1;
      if (tt < 1 / 6) return p + (q - p) * 6 * tt;
      if (tt < 1 / 2) return q;
      if (tt < 2 / 3) return p + (q - p) * (2 / 3 - tt) * 6;
      return p;
    };
    const to = (v) => Math.round(Math.min(1, Math.max(0, v)) * 255).toString(16).padStart(2, "0");
    return `#${to(channel(hh + 1 / 3))}${to(channel(hh))}${to(channel(hh - 1 / 3))}`;
  };
  return {
    accent: hex,
    ink: "#0c0d11",
    deep: hsl(h, Math.min(0.5, s * 0.45), 0.16),
    soft: hsl(h, Math.min(0.34, s * 0.3), 0.11),
  };
}

function entryId(entry) {
  return entry.id;
}

async function main() {
  const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
  const only = process.argv.slice(2).filter((a) => !a.startsWith("-"));
  const wanted = NEW_FRANCHISES.filter((f) => !only.length || only.includes(f.id));

  console.log("resolving release dates from Wikidata");
  await resolveDates(wanted.flatMap((f) => f.entries.map((e) => e.page)));
  for (const def of wanted) {
    console.log(`${def.id}: ${def.entries.length} entries`);
    const entries = [];
    for (const [index, e] of def.entries.entries()) {
      const lookup = cache[e.page];
      const date = DATE_FIXES[e.id] ?? lookup ?? e.date;
      if (!lookup && !DATE_FIXES[e.id]) console.log(`  ~ ${e.title}: no Wikidata date, using ${e.date}`);
      const entry = {
        id: entryId(e),
        title: e.title,
        date,
        year: Number(date.slice(0, 4)),
        kind: e.kind ?? "film",
        era: e.era ?? def.eraOrder[0],
        story: index + 1,
      };
      if (e.note) entry.note = e.note;
      if (e.optional) entry.optional = true;
      entries.push(entry);
    }

    const years = entries.map((e) => e.year);
    const franchise = {
      id: def.id,
      name: def.name,
      years: `${Math.min(...years)}â€“${Math.max(...years)}`,
      hook: def.hook,
      blurb: def.blurb,
      storyHint: def.storyHint,
      genre: def.genre,
      accent: def.accent,
      eraOrder: def.eraOrder,
      entries: entries.slice().sort((a, b) => a.date.localeCompare(b.date)),
    };

    const existing = catalog.franchises.findIndex((f) => f.id === def.id);
    if (existing >= 0) catalog.franchises[existing] = franchise;
    else catalog.franchises.push(franchise);
    catalog.accents[def.id] = accentTokens(def.accent);
    if (def.works?.length) {
      const works = def.works.map(({ page, ...rest }) => rest);
      catalog.works[def.id] = works;
    }
  }

  fs.writeFileSync(cachePath, JSON.stringify(cache, null, 2) + "\n");
  fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2) + "\n");

  // The book cover fetch needs the Wikipedia article for every work added above.
  const bookSources = {};
  for (const def of NEW_FRANCHISES) {
    for (const w of def.works ?? []) {
      bookSources[w.id] = { title: w.title, page: w.page, franchise: def.id };
    }
  }
  fs.writeFileSync(path.join(root, "book-sources.json"), JSON.stringify(bookSources, null, 2) + "\n");

  const entries = catalog.franchises.reduce((n, f) => n + f.entries.length, 0);
  const works = Object.values(catalog.works).reduce((n, w) => n + w.length, 0);
  console.log(
    `\n${catalog.franchises.length} franchises, ${entries} entries, ${works} books`,
  );
}

main();