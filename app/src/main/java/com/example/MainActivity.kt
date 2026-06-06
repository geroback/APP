package com.example

import android.os.Bundle
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TossBlue
import com.example.ui.theme.TossDarkGrey
import com.example.ui.theme.TossDivider
import com.example.ui.theme.TossGreen
import com.example.ui.theme.TossGrey
import com.example.ui.theme.TossLightGrey
import com.example.ui.theme.TossOrange
import com.example.ui.theme.TossRed
import com.example.ui.theme.TossWhite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          GameScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

// ----------------------------------------------------
// 3D 블록 & 데이터 모델 정의
// ----------------------------------------------------

enum class BlockType(val color: Color) {
  I(Color(0xFF00E5FF)), // Neon Royal Cyan
  O(Color(0xFFFFEA00)), // Neon Bright Yellow
  T(Color(0xFFD500F9)), // Neon Laser Violet
  L(Color(0xFFFF9100)), // Neon Flare Orange
  J(Color(0xFF2979FF)), // Neon Cobalt Blue
  S(Color(0xFF00E676)), // Neon Acid Lime Green
  Z(Color(0xFFFF1744))  // Neon Cyber Red
}

data class Block3D(
  val type: BlockType,
  val cubes: List<Triple<Int, Int, Int>>, // 상대적 (x, y, z) 오프셋
  val color: Color
) {
  // Rotate around X-axis (Pitch)
  fun rotateX(): Block3D {
    // (x, y, z) -> (x, -z, y)
    val rotated = cubes.map { Triple(it.first, -it.third, it.second) }
    return copy(cubes = rotated)
  }

  // Rotate around Y-axis (Yaw)
  fun rotateY(): Block3D {
    // (x, y, z) -> (z, y, -x)
    val rotated = cubes.map { Triple(it.third, it.second, -it.first) }
    return copy(cubes = rotated)
  }

  // Rotate around Z-axis (Roll)
  fun rotateZ(): Block3D {
    // (x, y, z) -> (-y, x, z)
    val rotated = cubes.map { Triple(-it.second, it.first, it.third) }
    return copy(cubes = rotated)
  }
}

fun createRandomBlock(): Block3D {
  val type = BlockType.values().random()
  val cubes = when (type) {
    BlockType.I -> listOf(
      Triple(0, 0, 0), Triple(0, 0, 1), Triple(0, 0, 2), Triple(0, 0, -1)
    )
    BlockType.O -> listOf(
      Triple(0, 0, 0), Triple(1, 0, 0), Triple(0, 1, 0), Triple(1, 1, 0)
    )
    BlockType.T -> listOf(
      Triple(0, 0, 0), Triple(-1, 0, 0), Triple(1, 0, 0), Triple(0, 1, 0)
    )
    BlockType.L -> listOf(
      Triple(0, 0, 0), Triple(0, -1, 0), Triple(0, 1, 0), Triple(1, 1, 0)
    )
    BlockType.J -> listOf(
      Triple(0, 0, 0), Triple(0, -1, 0), Triple(0, 1, 0), Triple(-1, 1, 0)
    )
    BlockType.S -> listOf(
      Triple(0, 0, 0), Triple(1, 0, 0), Triple(0, 1, 0), Triple(-1, 1, 0)
    )
    BlockType.Z -> listOf(
      Triple(0, 0, 0), Triple(-1, 0, 0), Triple(0, 1, 0), Triple(1, 1, 0)
    )
  }
  return Block3D(type, cubes, type.color)
}

data class Particle3D(
  var x: Float,
  var y: Float,
  var z: Float,
  val vx: Float,
  val vy: Float,
  val vz: Float,
  val color: Color,
  val size: Float,
  var alpha: Float = 1.0f,
  var life: Float = 1.0f,
  val decay: Float = 0.04f,
  val gravity: Float = 0.005f
)

// ----------------------------------------------------
// 게임 상태 관리 라이브 뷰모델 (ViewModel)
// ----------------------------------------------------

class GameViewModel : ViewModel() {
  val width = 5
  val height = 5
  val depth = 12

  private val _board = MutableStateFlow(Array(depth) { Array(height) { Array<Color?>(width) { null } } })
  val board: StateFlow<Array<Array<Array<Color?>>>> = _board.asStateFlow()

  private val _curBlockX = MutableStateFlow(2)
  val curBlockX: StateFlow<Int> = _curBlockX.asStateFlow()

  private val _curBlockY = MutableStateFlow(2)
  val curBlockY: StateFlow<Int> = _curBlockY.asStateFlow()

  private val _curBlockZ = MutableStateFlow(0)
  val curBlockZ: StateFlow<Int> = _curBlockZ.asStateFlow()

  private val _currentBlock = MutableStateFlow(createRandomBlock())
  val currentBlock: StateFlow<Block3D> = _currentBlock.asStateFlow()

  private val _nextBlock = MutableStateFlow(createRandomBlock())
  val nextBlock: StateFlow<Block3D> = _nextBlock.asStateFlow()

  private val _score = MutableStateFlow(0)
  val score: StateFlow<Int> = _score.asStateFlow()

  private val _level = MutableStateFlow(1)
  val level: StateFlow<Int> = _level.asStateFlow()

  private val _linesCleared = MutableStateFlow(0)
  val linesCleared: StateFlow<Int> = _linesCleared.asStateFlow()

  private val _isGameOver = MutableStateFlow(false)
  val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

  private val _isPaused = MutableStateFlow(false)
  val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _highScore = MutableStateFlow(0)
  val highScore: StateFlow<Int> = _highScore.asStateFlow()

  private val _isSoundOn = MutableStateFlow(true)
  val isSoundOn: StateFlow<Boolean> = _isSoundOn.asStateFlow()

  private val _particles = MutableStateFlow<List<Particle3D>>(emptyList())
  val particles: StateFlow<List<Particle3D>> = _particles.asStateFlow()

  private var gameJob: Job? = null
  private var particleJob: Job? = null
  private val random = java.util.Random()

  fun setSoundOn(on: Boolean) {
    _isSoundOn.value = on
  }

  fun togglePause() {
    _isPaused.value = !_isPaused.value
  }

  fun startGame() {
    _particles.value = emptyList()
    particleJob?.cancel()
    _board.value = Array(depth) { Array(height) { Array<Color?>(width) { null } } }
    _curBlockX.value = 2
    _curBlockY.value = 2
    _curBlockZ.value = 0
    _currentBlock.value = createRandomBlock()
    _nextBlock.value = createRandomBlock()
    _score.value = 0
    _level.value = 1
    _linesCleared.value = 0
    _isGameOver.value = false
    _isPaused.value = false
    _isPlaying.value = true

    startLoop()
  }

  private fun startLoop() {
    gameJob?.cancel()
    gameJob = kotlinx.coroutines.GlobalScope.launch {
      while (_isPlaying.value && !_isGameOver.value) {
        val delayTime = (1000 - (_level.value - 1) * 100).coerceAtLeast(200).toLong()
        delay(delayTime)
        if (!_isPaused.value) {
          moveDown()
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    gameJob?.cancel()
    particleJob?.cancel()
  }

  fun spawnParticles(zLayer: Int, layerColors: Array<Array<Color?>>) {
    val newParticles = mutableListOf<Particle3D>()
    for (yGrid in 0 until height) {
      for (xGrid in 0 until width) {
        val color = layerColors[yGrid][xGrid] ?: Color(0xFF00FF66)
        val basePx = xGrid - 2.0f
        val basePy = yGrid - 2.0f
        val basePz = zLayer.toFloat()
        
        // Spawning 8 particles per block for rich visual feedback
        for (i in 0 until 8) {
          val px = basePx + (random.nextFloat() - 0.5f) * 0.7f
          val py = basePy + (random.nextFloat() - 0.5f) * 0.7f
          val pz = basePz + (random.nextFloat() - 0.5f) * 0.7f
          
          val angle = random.nextFloat() * 2.0f * Math.PI.toFloat()
          val force = 0.05f + random.nextFloat() * 0.12f
          val vx = kotlin.math.cos(angle) * force
          val vy = kotlin.math.sin(angle) * force
          
          // Speed towards camera (negative vz to move towards z=0)
          val vz = -0.06f - random.nextFloat() * 0.18f
          
          val size = 0.05f + random.nextFloat() * 0.12f
          val decay = 0.015f + random.nextFloat() * 0.02f
          
          newParticles.add(
            Particle3D(
              x = px,
              y = py,
              z = pz,
              vx = vx,
              vy = vy,
              vz = vz,
              color = color,
              size = size,
              alpha = 1.0f,
              life = 1.0f,
              decay = decay,
              gravity = 0.0015f
            )
          )
        }
      }
    }
    
    _particles.value = _particles.value + newParticles
    triggerParticleUpdateLoop()
  }

  private fun triggerParticleUpdateLoop() {
    if (particleJob == null || particleJob?.isActive == false) {
      particleJob = kotlinx.coroutines.GlobalScope.launch {
        while (_particles.value.isNotEmpty() && _isPlaying.value) {
          delay(16) // ~60fps
          val current = _particles.value.mapNotNull { particle ->
            particle.x += particle.vx
            particle.y += particle.vy
            particle.z += particle.vz
            
            particle.y += particle.gravity
            
            particle.life -= particle.decay
            particle.alpha = particle.life.coerceIn(0f, 1f)
            
            if (particle.life > 0f) {
              particle
            } else {
              null
            }
          }
          _particles.value = current
        }
        particleJob = null
      }
    }
  }

  // 충돌 감지 로직
  private fun checkCollision(
    block: Block3D,
    bx: Int,
    by: Int,
    bz: Int
  ): Boolean {
    for (cube in block.cubes) {
      val targetX = bx + cube.first
      val targetY = by + cube.second
      val targetZ = bz + cube.third

      // 3D 우물 경계선 탈출 검사
      if (targetX < 0 || targetX >= width || targetY < 0 || targetY >= height) {
        return true
      }
      if (targetZ < 0) {
        continue // 터널 입구 위쪽은 아직 충돌하지 않은 것으로 간주
      }
      if (targetZ >= depth) {
        return true // 바닥에 도달
      }

      // 기존 고정된 블록과의 충돌 검사
      if (_board.value[targetZ][targetY][targetX] != null) {
        return true
      }
    }
    return false
  }

  // 좌우 상하 조작
  fun moveX(offset: Int) {
    if (!_isPlaying.value || _isPaused.value || _isGameOver.value) return
    val nextX = _curBlockX.value + offset
    if (!checkCollision(_currentBlock.value, nextX, _curBlockY.value, _curBlockZ.value)) {
      _curBlockX.value = nextX
    }
  }

  fun moveY(offset: Int) {
    if (!_isPlaying.value || _isPaused.value || _isGameOver.value) return
    val nextY = _curBlockY.value + offset
    if (!checkCollision(_currentBlock.value, _curBlockX.value, nextY, _curBlockZ.value)) {
      _curBlockY.value = nextY
    }
  }

  // 3D 회전 제어
  fun rotateBlock(axis: Char) {
    if (!_isPlaying.value || _isPaused.value || _isGameOver.value) return
    val rotated = when (axis) {
      'X' -> _currentBlock.value.rotateX()
      'Y' -> _currentBlock.value.rotateY()
      else -> _currentBlock.value.rotateZ()
    }

    // 회전 후 경계 이탈 보정 (Wall Kick)
    var testX = _curBlockX.value
    var testY = _curBlockY.value
    val displacementX = listOf(0, -1, 1, -2, 2)
    val displacementY = listOf(0, -1, 1, -2, 2)

    for (dx in displacementX) {
      for (dy in displacementY) {
        if (!checkCollision(rotated, testX + dx, testY + dy, _curBlockZ.value)) {
          _currentBlock.value = rotated
          _curBlockX.value = testX + dx
          _curBlockY.value = testY + dy
          return
        }
      }
    }
  }

  // 하강
  fun moveDown(): Boolean {
    if (!_isPlaying.value || _isPaused.value || _isGameOver.value) return false
    val nextZ = _curBlockZ.value + 1

    if (!checkCollision(_currentBlock.value, _curBlockX.value, _curBlockY.value, nextZ)) {
      _curBlockZ.value = nextZ
      return true
    } else {
      // 바닥에 충돌시 보드에 결합
      lockBlock()
      return false
    }
  }

  // 즉시 바닥으로 드롭 (Hard Drop)
  fun hardDrop() {
    if (!_isPlaying.value || _isPaused.value || _isGameOver.value) return
    while (moveDown()) {
      // 끝까지 떨어질 때까지 루프
    }
  }

  // 고정 처리 및 격자 줄 삭제 체크
  private fun lockBlock() {
    val currentBoard = _board.value
    // 주입
    for (cube in _currentBlock.value.cubes) {
      val targetX = _curBlockX.value + cube.first
      val targetY = _curBlockY.value + cube.second
      val targetZ = _curBlockZ.value + cube.third

      if (targetX in 0 until width && targetY in 0 until height && targetZ in 0 until depth) {
        currentBoard[targetZ][targetY][targetX] = _currentBlock.value.color
      }
    }

    // 격자가 고정된 후 UI 보드 상태 갱신 발동
    val newBoard = Array(depth) { z ->
      Array(height) { y ->
        Array(width) { x ->
          currentBoard[z][y][x]
        }
      }
    }
    _board.value = newBoard

    // 레이어 완전 소거 검사
    checkLayerClear()

    // 다음 블록 적재
    _currentBlock.value = _nextBlock.value
    _nextBlock.value = createRandomBlock()
    _curBlockX.value = 2
    _curBlockY.value = 2
    _curBlockZ.value = 0

    // 상단 오버플로우 체크 (게임 오버)
    if (checkCollision(_currentBlock.value, _curBlockX.value, _curBlockY.value, _curBlockZ.value)) {
      _isGameOver.value = true
      _isPlaying.value = false
      if (_score.value > _highScore.value) {
        _highScore.value = _score.value
      }
    }
  }

  private fun checkLayerClear() {
    val currentBoard = _board.value
    val layersToKeep = mutableListOf<Array<Array<Color?>>>()
    var clearedCount = 0

    // 깊은 곳(바닥, Z=11)부터 역순으로 조사
    for (z in 0 until depth) {
      var isLayerFull = true
      for (y in 0 until height) {
        for (x in 0 until width) {
          if (currentBoard[z][y][x] == null) {
            isLayerFull = false
            break
          }
        }
        if (!isLayerFull) break
      }

      if (isLayerFull) {
        clearedCount++
        spawnParticles(z, currentBoard[z])
      } else {
        layersToKeep.add(currentBoard[z])
      }
    }

    if (clearedCount > 0) {
      // 삭제된 수만큼 위에 빈 평면 레이어 축조
      val newBoardState = Array(depth) { Array(height) { Array<Color?>(width) { null } } }
      var targetZ = depth - 1

      // 남아있는 레이어를 아래서부터 차곡차곡 채우기
      for (keptLayer in layersToKeep.reversed()) {
        newBoardState[targetZ] = keptLayer
        targetZ--
      }

      _board.value = newBoardState
      _linesCleared.value += clearedCount
      // 점수 가산 (콤보 보너스 지수 적용)
      val points = when (clearedCount) {
        1 -> 100
        2 -> 300
        3 -> 600
        else -> 1000
      } * _level.value
      _score.value += points

      // 레벨 등반
      _level.value = (_linesCleared.value / 3) + 1
    }
  }
}

// ----------------------------------------------------
// UI 컴포저블: 메인 3D 테트리스 게임 스크린
// ----------------------------------------------------

@Composable
fun GameScreen(
  modifier: Modifier = Modifier,
  viewModel: GameViewModel = viewModel()
) {
  val board by viewModel.board.collectAsState()
  val curX by viewModel.curBlockX.collectAsState()
  val curY by viewModel.curBlockY.collectAsState()
  val curZ by viewModel.curBlockZ.collectAsState()
  val curBlock by viewModel.currentBlock.collectAsState()
  val nextBlock by viewModel.nextBlock.collectAsState()
  val score by viewModel.score.collectAsState()
  val level by viewModel.level.collectAsState()
  val linesCleared by viewModel.linesCleared.collectAsState()
  val isGameOver by viewModel.isGameOver.collectAsState()
  val isPaused by viewModel.isPaused.collectAsState()
  val isPlaying by viewModel.isPlaying.collectAsState()
  val highScore by viewModel.highScore.collectAsState()
  val isSoundOn by viewModel.isSoundOn.collectAsState()
  val particles by viewModel.particles.collectAsState()

  var showHelpDialog by remember { mutableStateOf(false) }

  // 주기적으로 펄스를 주면서 데모 플레이/기본 뷰를 보여주기 위한 처리
  LaunchedEffect(Unit) {
    viewModel.startGame()
  }

  if (showHelpDialog) {
    HelpDialog(onDismiss = { showHelpDialog = false })
  }

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  if (isLandscape) {
    Row(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF03060C)) // 전체 딥 블랙 배경 (여백 완전 배제)
    ) {
      // 1. 왼쪽 영역 (D-Pad 및 상태 대시보드) - 다크 콘솔 스타일
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1.0f)
          .background(Color(0xFF0D121A))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // [상단] 대시보드 (Score, High Score, Level)
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530))
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("SCORE", fontSize = 9.sp, color = Color(0xFF9EABB8), fontWeight = FontWeight.Bold)
              Text("LV.$level", fontSize = 9.sp, color = TossBlue, fontWeight = FontWeight.Bold)
            }
            Text("$score", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("HIGH: $highScore", fontSize = 8.sp, color = Color(0xFF6B7684))
          }
        }

        // [중간] Next Block 창 (높이 및 패딩 최적화)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530))
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("NEXT", fontSize = 9.sp, color = Color(0xFF9EABB8), fontWeight = FontWeight.Bold)
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                drawPreviewBlock(nextBlock)
              }
            }
          }
        }

        // [하단] D-Pad (양손 조작용 대형 컨트롤러 크로스 키)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E2530), RoundedCornerShape(20.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
          IconButton(
            onClick = { viewModel.moveY(-1) },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "위로", tint = Color.White, modifier = Modifier.size(32.dp))
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            IconButton(
              onClick = { viewModel.moveX(-1) },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "왼쪽", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Box(
              modifier = Modifier
                .size(10.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
            IconButton(
              onClick = { viewModel.moveX(1) },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "오른쪽", tint = Color.White, modifier = Modifier.size(32.dp))
            }
          }

          IconButton(
            onClick = { viewModel.moveY(1) },
            modifier = Modifier.size(48.dp)
          ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "아래", tint = Color.White, modifier = Modifier.size(32.dp))
          }
        }
      }

      // 2. 가운데 영역 (메인 3D 네온 터널 캔버스)
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1.3f)
          .background(Color(0xFF03060C))
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        val blurRadius = if (!isPlaying || isPaused || isGameOver) 12.dp else 0.dp
        
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF03060C))
            .border(1.dp, Color(0xFF0D1C1B), RoundedCornerShape(18.dp))
            .testTag("tunnel_canvas_box_landscape")
        ) {
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .blur(blurRadius)
          ) {
            draw3DTunnelGrid(viewModel.width, viewModel.height, viewModel.depth, curZ = curZ)
            drawLockedBlocks(board, viewModel.width, viewModel.height, viewModel.depth)
            drawActiveBlock(curBlock, curX, curY, curZ, viewModel.width, viewModel.height, viewModel.depth)
            drawParticles(particles)
          }

          if (!isPlaying || isPaused || isGameOver) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
              ) {
                Text(
                  text = if (isGameOver) "GAME OVER" else if (isPaused) "PAUSED" else "3D TUNNEL TETRIS",
                  color = if (isGameOver) Color(0xFFFF5252) else Color(0xFF00FFCC),
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = if (isGameOver) 24.sp else 18.sp,
                  textAlign = TextAlign.Center,
                  letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = if (isGameOver) "최종: ${score}점" else if (isPaused) "잠시 숨을 고르세요" else "3D 터널 입체 테트리스",
                  color = Color(0xFFE5E8EB),
                  fontSize = 11.sp,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                TossStyleButton(
                  text = if (isGameOver) "다시 도전하기" else "게임 이어하기",
                  onClick = {
                    if (isGameOver) {
                      viewModel.startGame()
                    } else if (isPaused) {
                      viewModel.togglePause()
                    } else {
                      viewModel.startGame()
                    }
                  },
                  tag = "action_resume_button_landscape"
                )
              }
            }
          }
        }
      }

      // 3. 오른쪽 영역 (회전 패드, 하강 조작, 시스템 제어 버튼들)
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .weight(1.0f)
          .background(Color(0xFF0D121A))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // [상단] 시스템 유틸리티 제어 (볼륨, 도움말, 재설정)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(Color(0xFF1E2530), CircleShape)
              .clickable { viewModel.setSoundOn(!isSoundOn) },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isSoundOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeMute,
              contentDescription = null,
              tint = TossBlue,
              modifier = Modifier.size(15.dp)
            )
          }
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(Color(0xFF1E2530), CircleShape)
              .clickable { showHelpDialog = true },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.HelpOutline,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
          }
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(Color(0xFF1E2530), CircleShape)
              .clickable { viewModel.startGame() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
          }
        }

        // [중간] X, Y, Z Rotational Action Pad
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("ROTATION CONTROL", fontSize = 8.sp, color = Color(0xFF6B7684), fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            LandscapeActionButton(
              icon = Icons.Rounded.RotateLeft,
              label = "P-X",
              onClick = { viewModel.rotateBlock('X') },
              color = Color(0xFF3182F6).copy(alpha = 0.15f),
              tint = Color(0xFF64D2FF)
            )
            LandscapeActionButton(
              icon = Icons.Rounded.Cached,
              label = "Y-Y",
              onClick = { viewModel.rotateBlock('Y') },
              color = Color(0xFF30D158).copy(alpha = 0.15f),
              tint = Color(0xFF30D158)
            )
            LandscapeActionButton(
              icon = Icons.Rounded.Refresh,
              label = "R-Z",
              onClick = { viewModel.rotateBlock('Z') },
              color = Color(0xFFFF9F0A).copy(alpha = 0.15f),
              tint = Color(0xFFFF9F0A)
            )
          }
        }

        // [하단] 하강 및 퀵 샌딩 제어 버튼 (엄지 패닝 편의 극대화)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LandscapeActionButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            label = "소프트",
            onClick = { viewModel.moveDown() },
            color = Color(0xFF2C323F),
            tint = Color.White,
            modifier = Modifier.weight(1f),
            isWide = true
          )
          LandscapeActionButton(
            icon = Icons.Rounded.South,
            label = "퀵 드롭",
            onClick = { viewModel.hardDrop() },
            color = TossBlue,
            tint = Color.White,
            modifier = Modifier.weight(1f),
            isWide = true
          )
        }
      }
    }
  } else {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(Color(0xFFF2F4F6))
        .padding(top = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
    // 1. 헤더 영역 (Toss 스타일 미니멀 탑바)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .height(56.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(
          text = "3D Tetris",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = Color(0xFF333D4B)
        )
        Text(
          text = "Front Perspective",
          fontSize = 11.sp,
          color = Color(0xFF6B7684),
          fontWeight = FontWeight.Bold
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Sound toggling with custom circular shadow button
        Box(
          modifier = Modifier
            .size(40.dp)
            .shadow(1.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { viewModel.setSoundOn(!isSoundOn) }
            .testTag("sound_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isSoundOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeMute,
            contentDescription = "Sound Toggle",
            tint = TossBlue,
            modifier = Modifier.size(20.dp)
          )
        }

        // Rule with custom circular shadow button
        Box(
          modifier = Modifier
            .size(40.dp)
            .shadow(1.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { showHelpDialog = true }
            .testTag("help_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.HelpOutline,
            contentDescription = "Show Rule",
            tint = Color(0xFF4E5968),
            modifier = Modifier.size(20.dp)
          )
        }

        // Restart with custom circular shadow button
        Box(
          modifier = Modifier
            .size(40.dp)
            .shadow(1.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { viewModel.startGame() }
            .testTag("restart_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Game Restart",
            tint = Color(0xFF4E5968),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // 2. 대시보드 영역 (점수판 및 다음 블록 미리보기)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 점수 카드
      Card(
        modifier = Modifier
          .weight(1.3f)
          .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SCORE",
              fontSize = 11.sp,
              color = Color(0xFF6B7684),
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "LV.$level",
              fontSize = 11.sp,
              color = TossBlue,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .background(TossBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = "$score",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333D4B)
          )

          Text(
            text = "최고 기록: $highScore",
            fontSize = 10.sp,
            color = Color(0xFF8B95A1),
            fontWeight = FontWeight.Bold
          )
        }
      }

      // 다음 블록 미리보기 카드
      Card(
        modifier = Modifier
          .weight(1f)
          .height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "NEXT",
            fontSize = 11.sp,
            color = Color(0xFF6B7684),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )

          // 3D Preview Canvas
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .aspectRatio(1.5f),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawPreviewBlock(nextBlock)
            }
          }
        }
      }
    }

    // 3. 메인 3D Perspective 터널 캔버스 (우물) - 외곽 플레이트는 프리미엄 화이트 바디로, 내부 터널 창만 딥다크 OLED 화면으로 연출해 토스/애플 감성의 일체감 완성
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .aspectRatio(1.0f)
        .shadow(4.dp, RoundedCornerShape(24.dp))
        .clip(RoundedCornerShape(24.dp))
        .background(Color.White)  // 대시보드 및 가상 패드 카드와 한 몸처럼 어우러지는 화이트 플레이트
        .border(1.dp, Color(0xFFE5E8EB), RoundedCornerShape(24.dp)) // 유려하고 세련된 토스식 실버 엣지
        .testTag("tunnel_canvas_box"),
      contentAlignment = Alignment.Center
    ) {
      val blurRadius = if (!isPlaying || isPaused || isGameOver) 12.dp else 0.dp
      
      // 안쪽 3D 터널 디스플레이 디바이스 윈도우 (OLED 베젤 연출)
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp) // 정갈한 모바일 기기 베젤 조율
          .clip(RoundedCornerShape(18.dp)) // 모퉁이가 둥근 화면 튜브 경계
          .background(Color(0xFF03060C)) // 완전한 다크 매트릭스 백스크린
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .blur(blurRadius)
        ) {
          draw3DTunnelGrid(viewModel.width, viewModel.height, viewModel.depth, curZ = curZ)
          drawLockedBlocks(board, viewModel.width, viewModel.height, viewModel.depth)
          // 현재 조작 중인 블록 렌더링
          drawActiveBlock(curBlock, curX, curY, curZ, viewModel.width, viewModel.height, viewModel.depth)
          drawParticles(particles)
        }

        // 우물 안 일시정지 또는 게임오버 오버레이 레이어 (스마트 스크린 내부에 정갈하게 조화)
        if (!isPlaying || isPaused || isGameOver) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.82f)), // 터널 안쪽 디바이스 스크린에만 녹아드는 깔끔한 다크 마스크
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.padding(24.dp)
            ) {
              Text(
                text = if (isGameOver) "GAME OVER" else if (isPaused) "PAUSED" else "3D TUNNEL TETRIS",
                color = if (isGameOver) Color(0xFFFF5252) else Color(0xFF00FFCC), // 일렉트릭 네온 로고 연계
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isGameOver) 30.sp else 22.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (isGameOver) "최종 점수: ${score}점" else if (isPaused) "잠시 숨을 고르고 도전하세요" else "도전적인 리얼 3D 공간을 정복하세요!",
                color = Color(0xFFE5E8EB),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                lineHeight = 20.sp
              )
              Spacer(modifier = Modifier.height(28.dp))

              TossStyleButton(
                text = if (isGameOver) "다시 도전하기" else "게임 이어하기",
                onClick = {
                  if (isGameOver) {
                    viewModel.startGame()
                  } else if (isPaused) {
                    viewModel.togglePause()
                  } else {
                    viewModel.startGame()
                  }
                },
                tag = "action_resume_button"
              )
            }
          }
        }
      }
    }

    // 4. 세련된 조작 가상 패드 패널(Gamepad Panel) - Crafted inside rounded-t-[40px] with outstanding iOS look
    Card(
      modifier = Modifier
        .fillMaxWidth(),
      shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // [좌측]: 상/하/좌/우 십자 방향 패드 (D-Pad)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .background(Color(0xFFF2F4F6), RoundedCornerShape(24.dp))
              .padding(8.dp)
          ) {
            IconButton(
              onClick = { viewModel.moveY(-1) },
              modifier = Modifier
                .size(44.dp)
                .testTag("dpad_up")
            ) {
              Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "위로 이동", tint = Color(0xFF333D4B))
            }

            Row {
              IconButton(
                onClick = { viewModel.moveX(-1) },
                modifier = Modifier
                  .size(44.dp)
                  .testTag("dpad_left")
              ) {
                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "왼쪽으로 이동", tint = Color(0xFF333D4B))
              }
              Spacer(modifier = Modifier.width(28.dp))
              IconButton(
                onClick = { viewModel.moveX(1) },
                modifier = Modifier
                  .size(44.dp)
                  .testTag("dpad_right")
              ) {
                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "오른쪽으로 이동", tint = Color(0xFF333D4B))
              }
            }

            IconButton(
              onClick = { viewModel.moveY(1) },
              modifier = Modifier
                .size(44.dp)
                .testTag("dpad_down")
            ) {
              Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "아래로 이동", tint = Color(0xFF333D4B))
            }
          }

          // [우측]: 3D Multi-Axis 회전 및 드롭 조작
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
          ) {
            // 회전 버튼 모음
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              ActionButton(
                icon = Icons.Rounded.RotateLeft,
                label = "Pitch X",
                onClick = { viewModel.rotateBlock('X') },
                color = TossBlue.copy(alpha = 0.08f),
                tint = TossBlue,
                tag = "rot_x_button"
              )
              ActionButton(
                icon = Icons.Rounded.Cached,
                label = "Yaw Y",
                onClick = { viewModel.rotateBlock('Y') },
                color = TossGreen.copy(alpha = 0.08f),
                tint = TossGreen,
                tag = "rot_y_button"
              )
              ActionButton(
                icon = Icons.Rounded.Refresh,
                label = "Roll Z",
                onClick = { viewModel.rotateBlock('Z') },
                color = TossOrange.copy(alpha = 0.08f),
                tint = TossOrange,
                tag = "rot_z_button"
              )
            }

            // 속도 강화 및 낙하 기동들
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // 소프트 하강
              ActionButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                label = "소프트",
                onClick = { viewModel.moveDown() },
                color = Color(0xFFF2F4F6),
                tint = Color(0xFF4D5662),
                tag = "soft_drop_button",
                isWide = true
              )
              // 하드 하강
              ActionButton(
                icon = Icons.Rounded.South,
                label = "퀵 드롭",
                onClick = { viewModel.hardDrop() },
                color = TossBlue,
                tint = Color.White,
                tag = "hard_drop_button",
                isWide = true
              )
            }
          }
        }

        // Bottom Home Indicator Area (OS-finished elegance)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
          modifier = Modifier
            .width(120.dp)
            .height(5.dp)
            .background(Color(0xFFE5E8EB), CircleShape)
        )
      }
    }
  }
  }
}

// ----------------------------------------------------
// 고정밀 3D Canvas 및 원근 투영 렌더러
// ----------------------------------------------------

fun Color.darker(factor: Float): Color {
  return Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
  )
}

fun Color.lighter(factor: Float): Color {
  return Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
  )
}

// 다각형 내부 채우기
fun DrawScope.drawFace(p1: Offset, p2: Offset, p3: Offset, p4: Offset, color: Color) {
  val path = Path().apply {
    moveTo(p1.x, p1.y)
    lineTo(p2.x, p2.y)
    lineTo(p3.x, p3.y)
    lineTo(p4.x, p4.y)
    close()
  }
  drawPath(path = path, color = color, style = Fill)
}

// 꼭짓점 연결 아웃라인 긋기
fun DrawScope.drawStroke(p1: Offset, p2: Offset, p3: Offset, p4: Offset, color: Color, strokeWidth: Float = 0.82f) {
  val path = Path().apply {
    moveTo(p1.x, p1.y)
    lineTo(p2.x, p2.y)
    lineTo(p3.x, p3.y)
    lineTo(p4.x, p4.y)
    close()
  }
  drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}

// 3D 큐브 그리기
fun DrawScope.drawCube(
  x: Float, y: Float, z: Float,
  color: Color,
  centerX: Float, centerY: Float,
  scaleMultiplier: Float,
  cameraDist: Float,
  shiftFactor: Float = 0f,
  isWireframe: Boolean = false
) {
  // 모퉁이 간 완만한 틈(Margin)을 두어 3D 격자의 고유 형태를 식별하기 쉽게 축소
  val halfSize = 0.46f

  val x0 = x - halfSize
  val x1 = x + halfSize
  val y0 = y - halfSize
  val y1 = y + halfSize
  val z0 = z + 0.04f
  val z1 = z + 0.96f

  val project = { px: Float, py: Float, pz: Float ->
    val s = cameraDist / (cameraDist + pz)
    Offset(
      centerX + (px + shiftFactor * pz) * scaleMultiplier * s,
      centerY + (py + shiftFactor * pz) * scaleMultiplier * s
    )
  }

  // 8개 모퉁이 포인트 투영 계산
  val p000 = project(x0, y0, z0)
  val p100 = project(x1, y0, z0)
  val p010 = project(x0, y1, z0)
  val p110 = project(x1, y1, z0)
  val p001 = project(x0, y0, z1)
  val p101 = project(x1, y0, z1)
  val p011 = project(x0, y1, z1)
  val p111 = project(x1, y1, z1)

  if (isWireframe) {
    // 3D 와이어프레임(이미지 그대로 선명한 화이트 가이드 라인) 모델 렌더링
    val strokeColor = Color.White
    val strokeWidth = 1.8.dp.toPx()

    // 아주 은은한 반투명 파스텔 조명을 가미해 매력적인 3D 유도체 홀로그램 장식
    val fillBg = color.copy(alpha = 0.16f)
    drawFace(p000, p100, p110, p010, fillBg)
    drawFace(p001, p101, p111, p011, fillBg.darker(0.12f))
    drawFace(p000, p100, p101, p001, fillBg.lighter(0.1f))
    drawFace(p010, p110, p111, p011, fillBg.darker(0.18f))

    // 전면/후면 3D 아웃라인 (선명한 화이트)
    drawStroke(p000, p100, p110, p010, strokeColor, strokeWidth)
    drawStroke(p001, p101, p111, p011, strokeColor, strokeWidth)

    // 세로 연결 기둥선들 (선명한 화이트)
    drawLine(color = strokeColor, start = p000, end = p001, strokeWidth = strokeWidth)
    drawLine(color = strokeColor, start = p100, end = p101, strokeWidth = strokeWidth)
    drawLine(color = strokeColor, start = p110, end = p111, strokeWidth = strokeWidth)
    drawLine(color = strokeColor, start = p010, end = p011, strokeWidth = strokeWidth)
  } else {
    // 일반 3D 솔리드 입체 큐브 렌더링 (어두운 포탈 속에서 입체 발광감이 돋보이는 셰이딩)
    // z축 깊이에 다른 물리적 반사 유도 (바닥면에 가까울수록 명반 감소)
    val maxZ = 12f
    val depthFactor = (1f - (z / maxZ) * 0.4f).coerceIn(0.42f, 1.0f)
    
    // 벽면에 붙었을 때 전술적인 명암대비를 강조하기 위한 추가 감쇄
    val isNearWall = kotlin.math.abs(x) >= 2.0f || kotlin.math.abs(y) >= 2.0f
    val shadowMultiplier = if (isNearWall) 0.85f else 1.0f
    val finalBrightness = depthFactor * shadowMultiplier
    
    val baseColor = color.darker(1f - finalBrightness)

    // 1. 입구 저편 뒷면 (Far Face)
    drawFace(p001, p101, p111, p011, baseColor.darker(0.35f))

    // 2. 사방 옆면 (조도 차를 명화처럼 선명히 주어 입체감을 극대화)
    drawFace(p000, p100, p101, p001, baseColor.lighter(0.16f)) // Top Face (가장 밝은 상계 조도)
    drawFace(p010, p110, p111, p011, baseColor.darker(0.24f)) // Bottom Face
    drawFace(p000, p010, p011, p001, baseColor.darker(0.12f)) // Left Face
    drawFace(p100, p110, p111, p101, baseColor.darker(0.06f)) // Right Face

    // 3. 눈 앞의 전면 (Near Face)
    drawFace(p000, p100, p110, p010, baseColor)

    // 4. 경계선 디자인 데코레이션 (어둡고 차가운 심연 배경에서 도드라지는 입체감 확보)
    val strokeColor = Color.Black.copy(alpha = 0.15f)
    drawStroke(p000, p100, p110, p010, strokeColor, 0.8.dp.toPx())
    drawStroke(p001, p101, p111, p011, strokeColor, 0.8.dp.toPx())

    val edgeColor = Color.White.copy(alpha = 0.42f)
    val edgeWidth = 0.9.dp.toPx()
    drawLine(color = edgeColor, start = p000, end = p001, strokeWidth = edgeWidth)
    drawLine(color = edgeColor, start = p100, end = p101, strokeWidth = edgeWidth)
    drawLine(color = edgeColor, start = p110, end = p111, strokeWidth = edgeWidth)
    drawLine(color = edgeColor, start = p010, end = p011, strokeWidth = edgeWidth)
  }
}

// 1. 터널 가이드 격자선 렌더링
fun DrawScope.draw3DTunnelGrid(width: Int, height: Int, depth: Int, curZ: Int = 0) {
  // 사용자가 공유한 이미지 완벽 대응: 칠흑 같은 매혹의 다크 스페이스 포탈 설정
  drawRect(color = Color(0xFF03060C))

  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val scaleMultiplier = min(size.width, size.height) * 0.44f
  val cameraDist = 3.2f

  val project = { dx: Float, dy: Float, dz: Float ->
    val s = cameraDist / (cameraDist + dz)
    Offset(
      centerX + dx * scaleMultiplier * s,
      centerY + dy * scaleMultiplier * s
    )
  }

  val endZ = depth.toFloat()
  val neonGreen = Color(0xFF00FF66) // 영롱한 일렉트릭 라이트 그린

  // 0. 터널 끝단 바닥면 백그라운드 솔리드 톤 처리 (3D 깊이의 감각을 극대화하는 딥 에메랄드 카본 세팅)
  val p00End = project(-2.5f, -2.5f, endZ)
  val p10End = project(2.5f, -2.5f, endZ)
  val p11End = project(2.5f, 2.5f, endZ)
  val p01End = project(-2.5f, 2.5f, endZ)
  drawFace(p00End, p10End, p11End, p01End, Color(0xFF0A120E))

  // 1. 관통하는 모서리 뼈대 프레임 및 각 벽면의 장기 종축 라인 (지속 종렬 종주 가이드)
  // 4개 모퉁이 관통선 (이미지와 일치하는 형광 그린)
  val cornersX = listOf(-2.5f, 2.5f)
  val cornersY = listOf(-2.5f, 2.5f)
  for (cx in cornersX) {
    for (cy in cornersY) {
      val start = project(cx, cy, 0f)
      val end = project(cx, cy, endZ)
      drawLine(
        color = neonGreen.copy(alpha = 0.85f),
        start = start,
        end = end,
        strokeWidth = 1.8.dp.toPx()
      )
    }
  }

  // 벽면의 가로세로를 5칸으로 쪼개주는 종축 가선들 (원근법을 유지시켜주는 선명한 네온 복도선)
  val gridCoords = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
  val neonLineColor = neonGreen.copy(alpha = 0.55f)
  for (coord in gridCoords) {
    // 윗벽 가이드선 (y = -2.5)
    drawLine(
      color = neonLineColor,
      start = project(coord, -2.5f, 0f),
      end = project(coord, -2.5f, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
    // 아랫벽 가이드선 (y = 2.5)
    drawLine(
      color = neonLineColor,
      start = project(coord, 2.5f, 0f),
      end = project(coord, 2.5f, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
    // 왼벽 가이드선 (x = -2.5)
    drawLine(
      color = neonLineColor,
      start = project(-2.5f, coord, 0f),
      end = project(-2.5f, coord, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
    // 오른벽 가이드선 (x = 2.5)
    drawLine(
      color = neonLineColor,
      start = project(2.5f, coord, 0f),
      end = project(2.5f, coord, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
  }

  // 2. 가로 방향 깊이 프레임 (동심원 사각형들)
  for (z in 0..depth) {
    val zFloat = z.toFloat()
    val alphaFactor = (1f - (zFloat / depth) * 0.45f).coerceIn(0.25f, 1.0f)
    
    // 현재 제어 중인 블록의 깊이와 일치하는 프레임에 입체 발광 가이드 아우라 부여
    val isCurrentZ = (z == curZ)
    val colorFrame = if (isCurrentZ) {
      Color(0xFF00FFCC).copy(alpha = 0.95f) // 고도로 빛나는 선명한 가이드 네온 에메랄드 서클
    } else {
      neonGreen.copy(alpha = 0.62f * alphaFactor)
    }

    val p00 = project(-2.5f, -2.5f, zFloat)
    val p10 = project(2.5f, -2.5f, zFloat)
    val p11 = project(2.5f, 2.5f, zFloat)
    val p01 = project(-2.5f, 2.5f, zFloat)

    // 외곽 장막 연결 테두리
    val frameWidth = if (isCurrentZ) {
      2.5.dp.toPx() // 기동 대상이 탑재된 깊이 장막은 도드라지는 볼륨의 특수 링 표지 처리
    } else if (z == 0 || z == (depth - 1)) {
      1.8.dp.toPx()
    } else {
      1.0.dp.toPx()
    }
    
    drawLine(colorFrame, p00, p10, strokeWidth = frameWidth)
    drawLine(colorFrame, p10, p11, strokeWidth = frameWidth)
    drawLine(colorFrame, p11, p01, strokeWidth = frameWidth)
    drawLine(colorFrame, p01, p00, strokeWidth = frameWidth)
  }

  // 3. 가장 저편 바닥 (End face) 내부 그리드선 그리기 (안착 지면 시안성 최적화)
  val colorBackGrid = neonGreen.copy(alpha = 0.72f)
  for (coord in gridCoords) {
    // 바닥 세로선
    drawLine(
      color = colorBackGrid,
      start = project(coord, -2.5f, endZ),
      end = project(coord, 2.5f, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
    // 바닥 가로선
    drawLine(
      color = colorBackGrid,
      start = project(-2.5f, coord, endZ),
      end = project(2.5f, coord, endZ),
      strokeWidth = 1.0.dp.toPx()
    )
  }
}

// 2. 쌓여서 굳어진 적층 블록 렌더링
fun DrawScope.drawLockedBlocks(
  board: Array<Array<Array<Color?>>>,
  width: Int,
  height: Int,
  depth: Int
) {
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val scaleMultiplier = min(size.width, size.height) * 0.44f
  val cameraDist = 3.2f

  // Painters algorithm: 깊이 깊은 바닥 Z가 큰 칸(먼 곳)부터 0(입구 방향)으로 올라오며 그림
  for (z in (depth - 1) downTo 0) {
    for (y in 0 until height) {
      for (x in 0 until width) {
        val color = board[z][y][x]
        if (color != null) {
          // grid 좌표 (x, y, z) -> 3D 공간 상의 중심좌표로 치환
          val physicsX = x - 2.0f
          val physicsY = y - 2.0f
          val physicsZ = z.toFloat()

          drawCube(
            x = physicsX,
            y = physicsY,
            z = physicsZ,
            color = color,
            centerX = centerX,
            centerY = centerY,
            scaleMultiplier = scaleMultiplier,
            cameraDist = cameraDist
          )
        }
      }
    }
  }
}

// 3. 기동중인 플레이어 활성 블록 드로잉
fun DrawScope.drawActiveBlock(
  block: Block3D,
  curX: Int,
  curY: Int,
  curZ: Int,
  width: Int,
  height: Int,
  depth: Int
) {
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val scaleMultiplier = min(size.width, size.height) * 0.44f
  val cameraDist = 3.2f

  // 블록의 모든 큐브 조각을 월드 좌표로 전사
  val mappedCubes = block.cubes.map { Triple(curX + it.first, curY + it.second, curZ + it.third) }
  // Z가 깊은 순(먼 곳)에서 정렬해서 Painters' 기법 조치
  val sortedCubes = mappedCubes.sortedByDescending { it.third }

  for (cube in sortedCubes) {
    val tx = cube.first
    val ty = cube.second
    val tz = cube.third

    if (tx in 0 until width && ty in 0 until height && tz in 0 until depth) {
      val physicsX = tx - 2.0f
      val physicsY = ty - 2.0f
      val physicsZ = tz.toFloat()

      drawCube(
        x = physicsX,
        y = physicsY,
        z = physicsZ,
        color = block.color,
        centerX = centerX,
        centerY = centerY,
        scaleMultiplier = scaleMultiplier,
        cameraDist = cameraDist,
        isWireframe = true
      )
    }
  }
}

// 4. 다음 블록 미리보기 전용 초소형 캔버스 드로잉
fun DrawScope.drawPreviewBlock(block: Block3D) {
  val centerX = size.width / 2f
  val centerY = size.height / 1.7f
  val scaleMultiplier = min(size.width, size.height) * 0.35f
  val cameraDist = 3f

  // 3D 궤적 내 큐브들을 깔끔하게 소환
  val sortedCubes = block.cubes.sortedByDescending { it.third }
  for (cube in sortedCubes) {
    val physicsX = cube.first.toFloat()
    val physicsY = cube.second.toFloat()
    val physicsZ = cube.third.toFloat()

    drawCube(
      x = physicsX,
      y = physicsY,
      z = physicsZ + 2.0f, // 미리보기 격자의 중간 바둑으로 배치
      color = block.color,
      centerX = centerX,
      centerY = centerY,
      scaleMultiplier = scaleMultiplier,
      cameraDist = cameraDist,
      shiftFactor = 0f // 미리보기는 격자가 비틀어지지 않게 정면으로 랜더링
    )
  }
}

fun DrawScope.drawParticleCube(
  x: Float, y: Float, z: Float,
  size: Float,
  color: Color,
  alpha: Float,
  centerX: Float, centerY: Float,
  scaleMultiplier: Float,
  cameraDist: Float
) {
  val halfSize = size / 2f
  val x0 = x - halfSize
  val x1 = x + halfSize
  val y0 = y - halfSize
  val y1 = y + halfSize
  val z0 = z - halfSize
  val z1 = z + halfSize

  val project = { px: Float, py: Float, pz: Float ->
    val s = cameraDist / (cameraDist + pz)
    Offset(
      centerX + px * scaleMultiplier * s,
      centerY + py * scaleMultiplier * s
    )
  }

  val p000 = project(x0, y0, z0)
  val p100 = project(x1, y0, z0)
  val p010 = project(x0, y1, z0)
  val p110 = project(x1, y1, z0)
  val p001 = project(x0, y0, z1)
  val p101 = project(x1, y0, z1)
  val p011 = project(x0, y1, z1)
  val p111 = project(x1, y1, z1)

  val baseColor = color.copy(alpha = alpha)

  // Draw 3D faces using drawFace helper
  drawFace(p001, p101, p111, p011, baseColor.darker(0.35f)) // Back Face
  drawFace(p000, p100, p101, p001, baseColor.lighter(0.16f)) // Top Face
  drawFace(p010, p110, p111, p011, baseColor.darker(0.24f)) // Bottom Face
  drawFace(p000, p010, p011, p001, baseColor.darker(0.12f)) // Left Face
  drawFace(p100, p110, p111, p101, baseColor.darker(0.06f)) // Right Face
  drawFace(p000, p100, p110, p010, baseColor) // Front Face

  // White stroke boundary for neon spark glow
  val strokeColor = Color.White.copy(alpha = 0.4f * alpha)
  val strokeWidth = 0.5.dp.toPx()
  drawStroke(p000, p100, p110, p010, strokeColor, strokeWidth)
}

fun DrawScope.drawParticles(particles: List<Particle3D>) {
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val scaleMultiplier = min(size.width, size.height) * 0.44f
  val cameraDist = 3.2f

  // Sort particles by Z in descending order (furthest away gets drawn first, Painters' algorithm)
  val sorted = particles.sortedByDescending { it.z }
  for (particle in sorted) {
    if (particle.z >= -3.0f && particle.z <= 16.0f) {
      drawParticleCube(
        x = particle.x,
        y = particle.y,
        z = particle.z,
        size = particle.size,
        color = particle.color,
        alpha = particle.alpha,
        centerX = centerX,
        centerY = centerY,
        scaleMultiplier = scaleMultiplier,
        cameraDist = cameraDist
      )
    }
  }
}

// ----------------------------------------------------
// UI 컴포넌트: 지침 맞춤형 반응형 인터랙티브 버튼들
// ----------------------------------------------------

@Composable
fun TossStyleButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tag: String = "action_button"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1.0f,
    animationSpec = spring(),
    label = "button_scale"
  )

  // 입체적인 인터랙티브 그라데이션 브러시
  val gradientBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3182F6), Color(0xFF1B64D3))
  )

  Box(
    modifier = modifier
      .scale(scale)
      .fillMaxWidth()
      .height(54.dp)
      .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        clip = false,
        ambientColor = Color(0xFF3182F6).copy(alpha = 0.35f),
        spotColor = Color(0xFF3182F6).copy(alpha = 0.45f)
      )
      .background(gradientBrush, RoundedCornerShape(24.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null, // 중첩 리플 효과 대신 깔끔한 스케일 반응에 올인하여 토스 스타일링 완성
        onClick = onClick
      )
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = Color.White,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 16.sp,
      letterSpacing = 0.8.sp
    )
  }
}

@Composable
fun LandscapeActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  color: Color,
  tint: Color,
  modifier: Modifier = Modifier,
  isWide: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1.0f,
    animationSpec = spring(),
    label = "landscape_btn_scale"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.scale(scale)
  ) {
    Box(
      modifier = Modifier
        .then(if (isWide) Modifier.fillMaxWidth().height(46.dp) else Modifier.size(58.dp, 52.dp))
        .clip(RoundedCornerShape(14.dp))
        .background(color)
        .clickable(
          interactionSource = interactionSource,
          indication = androidx.compose.foundation.LocalIndication.current,
          onClick = onClick
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF9EABB8)
    )
  }
}

@Composable
fun ActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  color: Color,
  tint: Color,
  tag: String,
  modifier: Modifier = Modifier,
  isWide: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.93f else 1.0f,
    animationSpec = spring(),
    label = "icon_button_scale"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.scale(scale)
  ) {
    Box(
      modifier = Modifier
        .size(if (isWide) 80.dp else 52.dp, 52.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(color)
        .clickable(
          interactionSource = interactionSource,
          indication = androidx.compose.foundation.LocalIndication.current,
          onClick = onClick
        )
        .testTag(tag),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      color = TossGrey
    )
  }
}

// ----------------------------------------------------
// UI 컴포넌트: 게임룰 도안 다이얼로그 (Dialog)
// ----------------------------------------------------

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = TossWhite),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "3D 터널 테트리스 안내",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TossDarkGrey,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          RuleRow("🎮 게임의 시점", "플레이어는 터널 입구에서 우물 바닥을 내다보는 3D 공간 시점입니다.")
          RuleRow("🕹️ 십자 제어 패드", "블록을 단면에 따라 상/하/좌/우로 움직여서 떨어뜨립니다.")
          RuleRow("🔄 3D 입체 회전", "Pitch X / Yaw Y / Roll Z 3축 입체 회전을 통해 사방으로 비틉니다.")
          RuleRow("💥 레이어 소거", "우물의 특정한 깊이 Z단면에 5x5 Grid (총 25칸)가 빈틈없이 들어차면, 해당 레이어가 시원하게 폭파하며 점수를 줍니다!")
        }

        Spacer(modifier = Modifier.height(24.dp))

        TossStyleButton(
          text = "알겠어요!",
          onClick = onDismiss,
          tag = "dialog_confirm_button"
        )
      }
    }
  }
}

@Composable
fun RuleRow(title: String, desc: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = title,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = TossBlue
    )
    Text(
      text = desc,
      fontSize = 12.sp,
      color = TossGrey,
      lineHeight = 16.sp
    )
  }
}

// ----------------------------------------------------
// 미리보기 활성화
// ----------------------------------------------------

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
  MyApplicationTheme {
    GameScreen()
  }
}

