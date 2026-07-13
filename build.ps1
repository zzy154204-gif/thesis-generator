# LaTeX 一键编译脚本（全部文档）
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

$docs = @("01_需求分析", "02_概要设计", "03_详细设计", "04_测试报告")

foreach ($doc in $docs) {
    $tex = Join-Path $doc "main.tex"
    if (Test-Path $tex) {
        Write-Host "=== 编译 $doc ===" -ForegroundColor Cyan
        Push-Location $doc
        xelatex -interaction=nonstopmode main.tex
        xelatex -interaction=nonstopmode main.tex
        Pop-Location
    }
}
Write-Host "`n全部编译完成！" -ForegroundColor Green
