package com.tugas.praktikum_cc

import android.app.Activity // <-- Import baru
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown // <-- Ikon baru untuk Hard Drop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // <-- Import baru
import androidx.compose.ui.platform.LocalDensity // <-- Import baru
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tugas.praktikum_cc.ui.theme.Praktikum_CCTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random // <-- Import baru

// --- Konfigurasi Game ---
const val COLS = 10
const val ROWS = 20
val EMPTY_COLOR = Color.DarkGray
val BORDER_COLOR = Color.Black

// --- Data Classes ---
data class Position(val x: Int, val y: Int)
data class Tetromino(val shape: List<List<Int>>, val color: Color)
// --- PERBAIKAN: Membuat ActivePiece sepenuhnya immutable ---
data class ActivePiece(val tetromino: Tetromino, val position: Position)

// --- Bentuk Balok (Tetromino) ---
object Tetrominos {
    // ... (Kode Tetrominos tidak berubah) ...
    val I = Tetromino(listOf(listOf(0, 0, 0, 0), listOf(1, 1, 1, 1), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0)), Color(0xFF00BCD4)) // Cyan
    val J = Tetromino(listOf(listOf(1, 0, 0), listOf(1, 1, 1), listOf(0, 0, 0)), Color(0xFF3F51B5)) // Blue
    val L = Tetromino(listOf(listOf(0, 0, 1), listOf(1, 1, 1), listOf(0, 0, 0)), Color(0xFFFF9800)) // Orange
    val O = Tetromino(listOf(listOf(1, 1), listOf(1, 1)), Color(0xFFFFEB3B)) // Yellow
    val S = Tetromino(listOf(listOf(0, 1, 1), listOf(1, 1, 0), listOf(0, 0, 0)), Color(0xFF4CAF50)) // Green
    val T = Tetromino(listOf(listOf(0, 1, 0), listOf(1, 1, 1), listOf(0, 0, 0)), Color(0xFF9C27B0)) // Purple
    val Z = Tetromino(listOf(listOf(1, 1, 0), listOf(0, 1, 1), listOf(0, 0, 0)), Color(0xFFE91E63)) // Red

    val list = listOf(I, J, L, O, S, T, Z)
}

// --- Status Aplikasi ---
sealed class GameState {
    // ... (Kode GameState tidak berubah) ...
    object MainMenu : GameState()
    object Playing : GameState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
// ... (Kode onCreate tidak berubah) ...
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Praktikum_CCTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // --- PERUBAHAN: Memanggil TetrisApp sebagai root ---
                    TetrisApp()
                }
            }
        }
    }
}

// --- Composable Root Baru ---
@Composable
fun TetrisApp() {
// ... (Kode TetrisApp tidak berubah) ...
    var gameState by remember { mutableStateOf<GameState>(GameState.MainMenu) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1a1a20)) {
        when (gameState) {
            GameState.MainMenu -> {
                MainMenuScreen(
                    onPlayClicked = { gameState = GameState.Playing }
                )
            }
            GameState.Playing -> {
                TetrisGameScreen(
                    onBackToMenu = { gameState = GameState.MainMenu }
                )
            }
        }
    }
}

// --- Composable Menu Utama Baru ---
@Composable
fun MainMenuScreen(onPlayClicked: () -> Unit) {
    // --- TAMBAHAN: Dapatkan konteks untuk menutup aplikasi ---
    val activity = (LocalContext.current as? Activity)

    // --- TAMBAHAN: State untuk background animasi ---
    val blockColors = remember { Tetrominos.list.map { it.color } }
    data class FallingBlock(val y: Float, val x: Float, val size: Float, val color: Color, val speed: Float)

    val fallingBlocks = remember { mutableStateListOf<FallingBlock>() }
    val density = LocalDensity.current.density

    // Inisialisasi blok
    LaunchedEffect(Unit) {
        if (fallingBlocks.isEmpty()) { // Hanya inisialisasi jika kosong
            for (i in 0..30) { // Tambah jumlah blok
                fallingBlocks.add(
                    FallingBlock(
                        y = Random.nextFloat() * -1500f, // Mulai di atas layar
                        x = Random.nextFloat(), // Persentase lebar (0.0 to 1.0)
                        size = Random.nextFloat() * 25f + 15f * density, // Buat lebih kecil
                        color = blockColors.random().copy(alpha = Random.nextFloat() * 0.2f + 0.1f), // Buat lebih transparan
                        speed = Random.nextFloat() * 1.0f + 0.5f // Buat lebih lambat
                    )
                )
            }
        }
    }

    BoxWithConstraints( // Gunakan BoxWithConstraints untuk mendapatkan tinggi
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- PERBAIKAN: Menggunakan 'this.constraints' secara eksplisit ---
        val screenHeight = this.constraints.maxHeight.toFloat()

        // Animasi blok jatuh
        LaunchedEffect(Unit) {
            while (true) {
                delay(16) // ~60 FPS

                fallingBlocks.forEachIndexed { index, block ->
                    val newY = block.y + block.speed

                    // Jika blok keluar layar di bawah, reset ke atas
                    if (newY > screenHeight) {
                        fallingBlocks[index] = fallingBlocks[index].copy(
                            y = -100f, // Reset ke atas
                            x = Random.nextFloat(),
                            color = blockColors.random().copy(alpha = Random.nextFloat() * 0.2f + 0.1f),
                            speed = Random.nextFloat() * 1.0f + 0.5f
                        )
                    } else {
                        fallingBlocks[index] = fallingBlocks[index].copy(y = newY)
                    }
                }
            }
        }

        // --- TAMBAHAN: Canvas untuk background ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            fallingBlocks.forEach { block ->
                drawRect(
                    color = block.color,
                    topLeft = Offset(block.x * size.width, block.y),
                    size = Size(block.size, block.size)
                )
            }
        }

        // Konten menu di atas background
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize() // Pastikan column mengisi agar center bekerja
        ) {
            Text(
                text = "TETRIS",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onPlayClicked,
                modifier = Modifier
                    .width(250.dp)
                    .height(70.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("PLAY", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }

            // --- TOMBOL QUIT BARU ---
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton( // Menggunakan OutlinedButton agar beda
                onClick = { activity?.finish() }, // Panggil finish()
                modifier = Modifier
                    .width(250.dp) // Samakan lebar
                    .height(70.dp), // Samakan tinggi
                shape = MaterialTheme.shapes.medium
            ) {
                Text("QUIT", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun TetrisGameScreen(onBackToMenu: () -> Unit) { // <-- Parameter baru
// ... (Kode state game tidak berubah) ...
    var score by remember { mutableIntStateOf(0) }
    var lines by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var dropInterval by remember { mutableLongStateOf(1000L) }
    var isPaused by remember { mutableStateOf(false) } // <-- State Jeda Baru

    // Papan permainan (board)
    val board = remember { mutableStateListOf<MutableList<Color>>() }

// ... (Kode inisialisasi papan tidak berubah) ...
    LaunchedEffect(Unit) {
        board.addAll(List(ROWS) { MutableList(COLS) { EMPTY_COLOR } })
    }

    // Fungsi untuk membuat papan kosong
    fun createEmptyBoard(): List<MutableList<Color>> {
        return List(ROWS) { MutableList(COLS) { EMPTY_COLOR } }
    }

    // Fungsi untuk membuat balok baru
    fun spawnPiece(): ActivePiece {
// ... (Kode spawnPiece tidak berubah) ...
        val tetromino = Tetrominos.list.random()
        val position = Position(COLS / 2 - tetromino.shape[0].size / 2, 0)
        return ActivePiece(tetromino, position)
    }

    var currentPiece by remember { mutableStateOf(spawnPiece()) }
    var nextPiece by remember { mutableStateOf(spawnPiece()) }

    // --- Logika Game ---

    // Cek apakah gerakan valid
    fun isValidMove(piece: ActivePiece, newPosition: Position, shape: List<List<Int>> = piece.tetromino.shape): Boolean {
// ... (Kode isValidMove tidak berubah) ...
        for (y in shape.indices) {
            for (x in shape[y].indices) {
                if (shape[y][x] > 0) {
                    val newX = newPosition.x + x
                    val newY = newPosition.y + y

                    // Cek batas dinding
                    if (newX < 0 || newX >= COLS || newY >= ROWS) return false
                    // Cek tumpukan balok
                    if (newY >= 0 && board.isNotEmpty() && board[newY][newX] != EMPTY_COLOR) return false
                }
            }
        }
        return true
    }

    // Kunci balok ke papan
    fun solidifyPiece() {
// ... (Kode solidifyPiece tidak berubah) ...
        currentPiece.tetromino.shape.forEachIndexed { y, row ->
            row.forEachIndexed { x, value ->
                if (value > 0) {
                    val boardX = currentPiece.position.x + x
                    val boardY = currentPiece.position.y + y
                    if (boardY >= 0 && boardY < ROWS && boardX >= 0 && boardX < COLS) {
                        board[boardY][boardX] = currentPiece.tetromino.color
                    }
                }
            }
        }
    }

    // Cek dan hapus baris
    fun checkLines() {
// ... (Kode checkLines tidak berubah) ...
        var linesCleared = 0
        var y = ROWS - 1
        while (y >= 0) {
            if (board.isNotEmpty() && board[y].all { it != EMPTY_COLOR }) {
                board.removeAt(y)
                board.add(0, MutableList(COLS) { EMPTY_COLOR })
                linesCleared++
            } else {
                y--
            }
        }
        if (linesCleared > 0) {
            lines += linesCleared
            score += linesCleared * 100
            // Percepat game setiap 5 baris
            if (lines % 5 == 0 && dropInterval > 100) {
                dropInterval -= 50
            }
        }
    }

    // Fungsi game over
    fun performGameOver() {
// ... (Kode performGameOver tidak berubah) ...
        isGameOver = true
    }

    // Fungsi tick (jatuh otomatis)
    fun onTick() {
// ... (Kode onTick tidak berubah) ...
        if (isGameOver || isPaused) return // <-- Ditambahkan cek isPaused
        val newPos = currentPiece.position.copy(y = currentPiece.position.y + 1)
        if (isValidMove(currentPiece, newPos)) {
            currentPiece = currentPiece.copy(position = newPos)
        } else {
            solidifyPiece()
            checkLines()
            currentPiece = nextPiece
            nextPiece = spawnPiece()
            if (!isValidMove(currentPiece, currentPiece.position)) {
                performGameOver()
            }
        }
    }

    // --- Kontrol ---
    fun movePiece(dx: Int) {
// ... (Kode movePiece tidak berubah) ...
        if (isGameOver || isPaused) return // <-- Ditambahkan cek isPaused
        val newPos = currentPiece.position.copy(x = currentPiece.position.x + dx)
        if (isValidMove(currentPiece, newPos)) {
            currentPiece = currentPiece.copy(position = newPos)
        }
    }

    fun rotatePiece() {
// ... (Kode rotatePiece tidak berubah) ...
        if (isGameOver || isPaused) return // <-- Ditambahkan cek isPaused
        val shape = currentPiece.tetromino.shape
        val newShape = shape[0].mapIndexed { index, _ ->
            shape.map { row -> row[index] }.reversed()
        }

        if (isValidMove(currentPiece, currentPiece.position, newShape)) {
            val newTetromino = currentPiece.tetromino.copy(shape = newShape)
            currentPiece = currentPiece.copy(tetromino = newTetromino)
        }
    }

    // --- FITUR BARU: Soft Drop ---
    fun softDrop() {
// ... (Kode softDrop tidak berubah) ...
        if (isGameOver || isPaused) return
        val newPos = currentPiece.position.copy(y = currentPiece.position.y + 1)
        if (isValidMove(currentPiece, newPos)) {
            currentPiece = currentPiece.copy(position = newPos)
            score += 1 // Tambahkan 1 poin untuk setiap langkah soft drop
        }
        // Jangan solidifikasi di sini, biarkan onTick() yang menanganinya
    }

    fun hardDrop() {
// ... (Kode hardDrop tidak berubah) ...
        if (isGameOver || isPaused) return // <-- Ditambahkan cek isPaused
        var finalPos = currentPiece.position
        while (isValidMove(currentPiece, finalPos.copy(y = finalPos.y + 1))) {
            finalPos = finalPos.copy(y = finalPos.y + 1)
            score += 2
        }

        currentPiece = currentPiece.copy(position = finalPos)

        // Panggil logika penguncian secara manual
        solidifyPiece()
        checkLines()
        currentPiece = nextPiece
        nextPiece = spawnPiece()
        if (!isValidMove(currentPiece, currentPiece.position)) {
            performGameOver()
        }
    }

    // Fungsi reset game
    fun resetGame() {
// ... (Kode resetGame tidak berubah) ...
        board.clear()
        board.addAll(createEmptyBoard())
        score = 0
        lines = 0
        dropInterval = 1000L
        currentPiece = spawnPiece()
        nextPiece = spawnPiece()
        isGameOver = false
        isPaused = false // <-- Pastikan reset juga status jeda
    }

    // Game Loop
    // --- PERUBAHAN: Game loop sekarang bergantung pada isPaused ---
    LaunchedEffect(key1 = isGameOver, key2 = isPaused) {
// ... (Kode LaunchedEffect tidak berubah) ...
        if (!isGameOver && !isPaused) {
            while (isActive) {
                delay(dropInterval)
                onTick()
            }
        }
    }

    // --- UI (Tampilan) ---
    // --- PERUBAHAN: Scaffold tidak lagi digunakan agar lebih fleksibel ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
// ... (Kode Column tidak berubah) ...
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Info Panel
            Row(
// ... (Kode Row Info Panel tidak berubah) ...
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBox("Skor", score.toString())
                InfoBox("Baris", lines.toString())
            }

            // Game Area
            Box(modifier = Modifier.weight(1f).aspectRatio(COLS.toFloat() / ROWS.toFloat())) {
// ... (Kode Game Area tidak berubah) ...
                GameCanvas(board, currentPiece)
                // Overlay Game Over (hanya tampil jika game over dan tidak dijeda)
                if (isGameOver) {
                    GameOverOverlay(onRestart = { resetGame() })
                }
            }

            // Control Panel
            GameControls(
                onMoveLeft = { movePiece(-1) },
                onMoveRight = { movePiece(1) },
                onRotate = { rotatePiece() },
                onDrop = { hardDrop() },
                onSoftDrop = { softDrop() } // <-- Mengirim fungsi softDrop
            )
        }

        // --- Tombol Jeda (Pause Button) ---
        // Tampil jika tidak game over dan tidak dijeda
        if (!isGameOver && !isPaused) {
// ... (Kode Tombol Jeda tidak berubah) ...
            IconButton(
                onClick = { isPaused = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Jeda",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // --- Overlay Jeda (Pause Overlay) ---
        // Tampil hanya jika dijeda
        if (isPaused) {
// ... (Kode Overlay Jeda tidak berubah) ...
            PauseMenuOverlay(
                onResume = { isPaused = false },
                onRestart = { resetGame() }, // resetGame() akan otomatis set isPaused = false
                onBackToMenu = onBackToMenu
            )
        }
    }
}

// --- Composable Menu Jeda Baru ---
@Composable
fun PauseMenuOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
// ... (Kode PauseMenuOverlay tidak berubah) ...
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)), // Lebih gelap
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("DIJEDA", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onResume, modifier = Modifier.width(200.dp)) {
                Text("Lanjutkan", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRestart, modifier = Modifier.width(200.dp)) {
                Text("Mulai Ulang", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onBackToMenu, modifier = Modifier.width(200.dp)) {
                Text("Kembali ke Menu", fontSize = 18.sp)
            }
        }
    }
}


@Composable
fun InfoBox(title: String, value: String) {
// ... (Kode InfoBox tidak berubah) ...
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 16.sp, color = Color.Gray)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White) // Warna teks diubah
    }
}

@Composable
fun GameCanvas(board: List<List<Color>>, piece: ActivePiece) {
// ... (Kode GameCanvas tidak berubah) ...
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF1a1a20))) {
        val blockSize = size.width / COLS

        // Gambar papan
        board.forEachIndexed { y, row ->
            row.forEachIndexed { x, color ->
                drawRect(
                    color = color,
                    topLeft = Offset(x * blockSize, y * blockSize),
                    size = Size(blockSize, blockSize)
                )
                drawRect(
                    color = BORDER_COLOR,
                    topLeft = Offset(x * blockSize, y * blockSize),
                    size = Size(blockSize, blockSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Gambar balok aktif
        piece.tetromino.shape.forEachIndexed { y, row ->
            row.forEachIndexed { x, value ->
                if (value > 0) {
                    drawRect(
                        color = piece.tetromino.color,
                        topLeft = Offset((piece.position.x + x) * blockSize, (piece.position.y + y) * blockSize),
                        size = Size(blockSize, blockSize)
                    )
                }
            }
        }
    }
}

@Composable
fun GameControls(
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit,
    onSoftDrop: () -> Unit // <-- Parameter baru
) {
// ... (Kode GameControls tidak berubah) ...
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- BARIS PERTAMA: KONTROL GERAKAN ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // Sekarang mengatur 4 tombol
        ) {
            ControlButton(icon = Icons.AutoMirrored.Filled.ArrowLeft, onClick = onMoveLeft)
            ControlButton(icon = Icons.Default.ArrowDownward, onClick = onSoftDrop) // <-- TOMBOL BARU
            ControlButton(icon = Icons.AutoMirrored.Filled.RotateRight, onClick = onRotate)
            ControlButton(icon = Icons.AutoMirrored.Filled.ArrowRight, onClick = onMoveRight)
        }
        Spacer(Modifier.height(16.dp))

        // --- BARIS KEDUA: HARD DROP ---
        Button(
            onClick = onDrop,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            // --- Ikon diubah agar berbeda dari soft drop ---
            Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Jatuhkan")
            Text("JATUHKAN", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
// ... (Kode ControlButton tidak berubah) ...
    Button(
        onClick = onClick,
        modifier = Modifier.size(70.dp)
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
fun GameOverOverlay(onRestart: () -> Unit) {
// ... (Kode GameOverOverlay tidak berubah) ...
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)), // Lebih gelap
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White) // Ukuran disamakan
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRestart, modifier = Modifier.width(200.dp)) { // Lebar disamakan
                Icon(Icons.Default.Refresh, contentDescription = "Mulai Ulang")
                Text("Mulai Ulang", modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp)
            }
        }
    }
}

