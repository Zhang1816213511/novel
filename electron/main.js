const { app, BrowserWindow, dialog, ipcMain } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');
const fs = require('fs');

let mainWindow = null;
let javaProcess = null;
const isDev = !app.isPackaged;

// ─── 路径 ────────────────────────────────────────────────
function getBackendPaths() {
  if (isDev) {
    // 开发模式：使用系统 java 命令 + Maven target 目录
    return {
      java: 'java',
      jar: path.join(__dirname, '..', 'target', 'novel-1.0.0.jar'),
    };
  }
  // 生产模式：使用 bundle 进来的 JRE 和 jar
  const res = process.resourcesPath;
  return {
    java: path.join(res, 'backend', 'runtime', 'bin', 'java.exe'),
    jar: path.join(res, 'backend', 'novel-1.0.0.jar'),
  };
}

// 数据目录：用户 AppData 下
function getDataDir() {
  return path.join(app.getPath('userData'), 'data');
}

// ─── 等待后端就绪 ────────────────────────────────────────
function waitForServer(url, timeoutMs = 60000) {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const tryConnect = () => {
      const req = http.get(url, () => {
        req.destroy();
        resolve();
      });
      req.on('error', () => {
        if (Date.now() - start > timeoutMs) {
          reject(new Error(`后端启动超时 (${timeoutMs / 1000}s)`));
        } else {
          setTimeout(tryConnect, 600);
        }
      });
      req.setTimeout(3000, () => {
        req.destroy();
        setTimeout(tryConnect, 600);
      });
    };
    tryConnect();
  });
}

// ─── 启动 Java 后端 ──────────────────────────────────────
function startBackend() {
  return new Promise((resolve, reject) => {
    const { java, jar } = getBackendPaths();

    if (!fs.existsSync(jar)) {
      reject(new Error(`未找到 JAR 文件: ${jar}\n请先执行前端+后端构建`));
      return;
    }
    // 开发模式下检查 java 命令
    if (isDev) {
      const which = spawn('where', ['java']);
      which.on('error', () => {
        reject(new Error('未找到 java 命令，请确保 JAVA_HOME 已配置'));
      });
    }
    if (!isDev && !fs.existsSync(java)) {
      reject(new Error(`未找到 JRE: ${java}`));
      return;
    }

    const dataDir = getDataDir();
    fs.mkdirSync(dataDir, { recursive: true });

    console.log(`[启动] 后端 JAR : ${jar}`);
    console.log(`[启动] Java     : ${java}`);
    console.log(`[启动] 数据目录 : ${dataDir}`);

    javaProcess = spawn(java, [
      `-Dapp.datadir=${dataDir}`,
      '-jar', jar,
    ], { stdio: ['pipe', 'pipe', 'pipe'] });

    javaProcess.stdout.on('data', d =>
      console.log(`[Backend] ${d.toString().trim()}`)
    );
    javaProcess.stderr.on('data', d =>
      console.error(`[Backend] ${d.toString().trim()}`)
    );
    javaProcess.on('error', err =>
      reject(new Error(`启动 Java 进程失败: ${err.message}`))
    );
    javaProcess.on('exit', code => {
      console.log(`[Backend] 进程退出, code=${code}`);
      javaProcess = null;
    });

    waitForServer('http://localhost:8080')
      .then(resolve)
      .catch(err => {
        // 尝试兜底 — 有些 Spring Boot 项目启动慢
        waitForServer('http://localhost:8080', 30000)
          .then(resolve)
          .catch(reject);
      });
  });
}

// ─── 强制关闭后端 ────────────────────────────────────────
function killBackend() {
  if (!javaProcess) return;
  console.log('[关闭] 正在停止后端...');
  javaProcess.kill('SIGTERM');
  setTimeout(() => {
    if (javaProcess) {
      javaProcess.kill('SIGKILL');
      javaProcess = null;
    }
  }, 3000);
}

// ─── 创建主窗口 ──────────────────────────────────────────
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    title: 'NovelWriter',
    icon: path.join(__dirname, 'icon.ico'),
    frame: false,
    backgroundColor: '#1e293b',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    show: false,
  });

  mainWindow.loadURL('http://localhost:8080');
  mainWindow.once('ready-to-show', () => mainWindow.show());
  mainWindow.on('closed', () => { mainWindow = null; });
  mainWindow.on('maximize', () => mainWindow?.webContents.send('window-maximize-change', true));
  mainWindow.on('unmaximize', () => mainWindow?.webContents.send('window-maximize-change', false));
}

// ─── 窗口控制 IPC ────────────────────────────────────────
ipcMain.on('window-minimize', () => mainWindow?.minimize());
ipcMain.on('window-maximize', () => {
  if (mainWindow?.isMaximized()) mainWindow.unmaximize();
  else mainWindow?.maximize();
});
ipcMain.on('window-close', () => mainWindow?.close());
ipcMain.handle('window-is-maximized', () => mainWindow?.isMaximized());

// ─── App 生命周期 ────────────────────────────────────────
app.whenReady().then(async () => {
  try {
    await startBackend();
    createWindow();
  } catch (err) {
    dialog.showErrorBox('启动失败', err.message);
    app.quit();
  }
});

app.on('window-all-closed', () => {
  killBackend();
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', killBackend);

app.on('activate', () => {
  if (mainWindow === null && javaProcess) createWindow();
});
