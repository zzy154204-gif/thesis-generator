# LaTeX 编译脚本 - 双击运行或 PowerShell 执行
# 使用方法: .\build.ps1
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$texfile = "需求分析文档.tex"
Write-Host "正在编译 $texfile ..." -ForegroundColor Cyan
xelatex -interaction=nonstopmode $texfile
xelatex -interaction=nonstopmode $texfile  # 第二次编译生成目录
Write-Host "编译完成！PDF 已生成。" -ForegroundColor Green
