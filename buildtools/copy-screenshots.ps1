param(
  [string]$PaparazziDir = "C:\Projects\Orderly\app\build\reports\paparazzi\debug",
  [string]$OutDir = "C:\Projects\Orderly\screenshots"
)

# Maps each Paparazzi snapshot name to the file name used in the repo.
$names = [ordered]@{
  "home"           = "01-home.png"
  "shell"          = "02-shell-navigation.png"
  "library-grid"   = "03-library-grid.png"
  "library-list"   = "04-library-list.png"
  "upcoming"       = "05-upcoming.png"
  "search-browse"  = "06-search-browse.png"
  "search-results" = "07-search-results.png"
  "detail-watch"   = "08-detail-watch.png"
  "detail-books"   = "09-detail-books.png"
  "settings"       = "10-settings.png"
  "home-light"     = "11-home-light.png"
  "home-genre"     = "12-home-genre.png"
}

$map = @{}
foreach ($f in Get-ChildItem (Join-Path $PaparazziDir "runs") -Filter *.js) {
  $t = Get-Content $f.FullName -Raw
  [regex]::Matches($t, '"name":\s*"([^"]+)"[\s\S]*?"file":\s*"images/([a-f0-9]+)\.png"') | ForEach-Object {
    $map[$_.Groups[1].Value] = $_.Groups[2].Value
  }
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
foreach ($key in $names.Keys) {
  $hash = $map[$key]
  if (-not $hash) { Write-Output "MISSING  $key"; continue }
  $src = Join-Path (Join-Path $PaparazziDir "images") "$hash.png"
  if (-not (Test-Path $src)) { Write-Output "MISSING  $key ($src)"; continue }
  $dest = Join-Path $OutDir $names[$key]
  Copy-Item $src $dest -Force
  Write-Output ("{0,-16} -> {1,-26} {2} bytes" -f $key, $names[$key], (Get-Item $src).Length)
}