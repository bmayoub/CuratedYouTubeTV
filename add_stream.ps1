param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Name,
    
    [Parameter(Mandatory=$true, Position=1)]
    [string]$Url,
    
    [switch]$Push
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($Push) {
    python "$scriptDir\add_stream.py" $Name $Url -Push
} else {
    python "$scriptDir\add_stream.py" $Name $Url
}
