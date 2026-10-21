$ErrorActionPreference = 'Stop'
$probeRoot = 'C:\Users\ernes\tp\_temp\icon236-native-20261021\verified'
$javaBin = 'C:\Program Files\Java\jdk-25.0.4\bin\java.exe'
$outputDirectory = Join-Path $probeRoot 'results'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public static class Icon236Windows {
    public delegate bool EnumCallback(IntPtr handle, IntPtr parameter);
    [StructLayout(LayoutKind.Sequential)] public struct Rect { public int Left, Top, Right, Bottom; }
    public class Window {
        public long Handle; public string Title; public int Left, Top, Width, Height; public uint Dpi;
    }
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumCallback callback, IntPtr parameter);
    [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr handle);
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] static extern int GetWindowText(IntPtr handle, StringBuilder text, int max);
    [DllImport("user32.dll")] static extern bool GetWindowRect(IntPtr handle, out Rect rect);
    [DllImport("user32.dll")] static extern uint GetDpiForWindow(IntPtr handle);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr handle, int command);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr handle, IntPtr device, uint flags);
    [DllImport("user32.dll", EntryPoint="IsWindowVisible")] public static extern bool IsWindowVisibleForCapture(IntPtr handle);
    [DllImport("user32.dll")] public static extern IntPtr SendMessage(IntPtr handle, uint message, IntPtr word, IntPtr parameter);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr handle, uint message, IntPtr word, IntPtr parameter);
    [DllImport("user32.dll")] public static extern IntPtr SetThreadDpiAwarenessContext(IntPtr context);
    public static Window[] Find(uint targetProcess) {
        var result = new List<Window>();
        EnumWindows((handle, parameter) => {
            uint owner; GetWindowThreadProcessId(handle, out owner);
            if (owner == targetProcess) {
                var text = new StringBuilder(512); GetWindowText(handle, text, text.Capacity);
                Rect rect;
                if (GetWindowRect(handle, out rect) && rect.Right > rect.Left && rect.Bottom > rect.Top)
                    result.Add(new Window { Handle=handle.ToInt64(), Title=text.ToString(), Left=rect.Left,
                        Top=rect.Top, Width=rect.Right-rect.Left, Height=rect.Bottom-rect.Top, Dpi=GetDpiForWindow(handle) });
            }
            return true;
        }, IntPtr.Zero);
        return result.ToArray();
    }
}
'@

function Wait-TestWindow($process, [string] $title) {
    $deadline = [DateTime]::UtcNow.AddSeconds(25)
    do {
        $process.Refresh()
        if ($process.HasExited) { throw "Test process exited before window appeared: $title" }
        $found = @([Icon236Windows]::Find($process.Id) | Where-Object Title -EQ $title)
        if ($found.Count -eq 1) { return $found[0] }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "No unique visible window for the test process: $title"
}

function Save-TestWindow($process, [string] $title, [string] $name) {
    $window = Wait-TestWindow $process $title
    $handle = [IntPtr]$window.Handle
    [void][Icon236Windows]::ShowWindow($handle, 9)
    [void][Icon236Windows]::SetForegroundWindow($handle)
    Start-Sleep -Milliseconds 900
    if (-not [Icon236Windows]::IsWindowVisibleForCapture($handle)) {
        throw "The test window is not visible: $title"
    }
    $window = Wait-TestWindow $process $title
    $bitmap = [System.Drawing.Bitmap]::new($window.Width, $window.Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $device = $graphics.GetHdc()
        try {
            if (-not [Icon236Windows]::PrintWindow($handle, $device, 2)) {
                throw "Windows could not capture the test window: $title"
            }
        } finally { $graphics.ReleaseHdc($device) }
        $bitmap.Save((Join-Path $outputDirectory "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $clip = $bitmap.Clone([System.Drawing.Rectangle]::new(0,0,$window.Width,[Math]::Min(90,$window.Height)), $bitmap.PixelFormat)
        try { $clip.Save((Join-Path $outputDirectory "$name-titlebar.png"), [System.Drawing.Imaging.ImageFormat]::Png) }
        finally { $clip.Dispose() }
    } finally { $graphics.Dispose(); $bitmap.Dispose() }
    $icon = [Icon236Windows]::SendMessage($handle, 0x007F, [IntPtr]2, [IntPtr]::Zero)
    if ($icon -eq [IntPtr]::Zero) { $icon = [Icon236Windows]::SendMessage($handle, 0x007F, [IntPtr]0, [IntPtr]::Zero) }
    if ($icon -ne [IntPtr]::Zero) {
        $iconBitmap = [System.Drawing.Icon]::FromHandle($icon).ToBitmap()
        try { $iconBitmap.Save((Join-Path $outputDirectory "$name-native-icon.png"), [System.Drawing.Imaging.ImageFormat]::Png) }
        finally { $iconBitmap.Dispose() }
    }
    return $window
}

$previousDpiContext = [Icon236Windows]::SetThreadDpiAwarenessContext([IntPtr](-4))
$normalProcess = $null
$dialogProcess = $null
try {
    $normalProcess = Start-Process -FilePath $javaBin -ArgumentList @('-jar','ponhub.jar') -WorkingDirectory "$probeRoot\normal" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$outputDirectory\normal.log" -RedirectStandardError "$outputDirectory\normal-error.log"
    $normalWindow = Save-TestWindow $normalProcess 'PonHub' 'main-window'
    [void][Icon236Windows]::PostMessage([IntPtr]$normalWindow.Handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
    if (-not $normalProcess.WaitForExit(15000)) { throw 'The normal test app did not close.' }
    if (Test-Path -LiteralPath "$probeRoot\normal\data\ponhub.json") { throw 'Empty-session test unexpectedly created operational data.' }
    $dialogProcess = Start-Process -FilePath $javaBin -ArgumentList @('-cp','ponhub.jar;classes','seedu.address.ui.Icon236NativeCheck') -WorkingDirectory "$probeRoot\dialog" -WindowStyle Hidden -PassThru -RedirectStandardOutput "$outputDirectory\dialog.log" -RedirectStandardError "$outputDirectory\dialog-error.log"
    $dialogWindow = Save-TestWindow $dialogProcess 'PonHub icon check' 'error-dialog'
    [void][Icon236Windows]::PostMessage([IntPtr]$dialogWindow.Handle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero)
    if (-not $dialogProcess.WaitForExit(15000)) { throw 'The dialog test app did not close.' }
    [pscustomobject]@{
        JarSha256=(Get-FileHash -LiteralPath "$probeRoot\normal\ponhub.jar" -Algorithm SHA256).Hash
        MainWindow=$normalWindow; ErrorDialog=$dialogWindow
        Screens=@([System.Windows.Forms.Screen]::AllScreens | ForEach-Object { $_.Bounds.ToString() })
        Note='Visible native windows captured with Win32 PrintWindow; WM_GETICON extracts the actual small icons. No taskbar capture or native keyboard/mouse acceptance is claimed.'
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath "$outputDirectory\windows.json" -Encoding utf8
    Write-Output "Native captures and environment saved to $outputDirectory"
} finally {
    foreach ($testProcess in @($normalProcess,$dialogProcess)) {
        if ($null -ne $testProcess) {
            $testProcess.Refresh()
            if (-not $testProcess.HasExited) { Stop-Process -Id $testProcess.Id }
        }
    }
    [void][Icon236Windows]::SetThreadDpiAwarenessContext($previousDpiContext)
}
