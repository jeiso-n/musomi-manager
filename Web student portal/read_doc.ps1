Add-Type -AssemblyName System.IO.Compression.FileSystem
$docPath = Resolve-Path "Musomi Prompt.docx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($docPath)
$entry = $zip.GetEntry("word/document.xml")
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xmlContent = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()

# Extract all text elements <w:t>
[xml]$xml = $xmlContent
$ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
$paragraphs = $xml.SelectNodes("//w:p", $ns)

$output = foreach ($p in $paragraphs) {
    $texts = $p.SelectNodes(".//w:t", $ns) | ForEach-Object { $_.InnerText }
    $texts -join ""
}

$output -join "`r`n" | Out-File -FilePath "extracted_prompt.txt" -Encoding utf8
Write-Output "Extracted successfully"
