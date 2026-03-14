<?php
// 手动解析 QUERY_STRING（兼容 CLI 模式）
if (getenv('QUERY_STRING') !== false) {
    parse_str(getenv('QUERY_STRING'), $getParams);
    $_GET = array_merge($_GET, $getParams);
}

// 获取参数并进行必要的转换（例如将空格还原为加号）
$a = isset($_GET['a']) ? floatval($_GET['a']) : 0;
$b = isset($_GET['b']) ? floatval($_GET['b']) : 0;
$op = isset($_GET['op']) ? $_GET['op'] : '+';

// 如果操作符是空格（由未编码的 + 导致），将其还原为加号
if ($op === ' ') {
    $op = '+';
}

// 计算结果
switch ($op) {
    case '+':
        $result = $a + $b;
        $op_name = '加法';
        break;
    case '-':
        $result = $a - $b;
        $op_name = '减法';
        break;
    case '*':
        $result = $a * $b;
        $op_name = '乘法';
        break;
    case '/':
        $result = $b != 0 ? $a / $b : '除数不能为0';
        $op_name = '除法';
        break;
    default:
        $result = '不支持的操作符';
        $op_name = '未知';
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>PHP 计算器测试</title>
    <style>
        body { font-family: Arial; margin: 20px; }
        .result { background: #e8f5e8; padding: 20px; border-radius: 5px; margin: 20px 0; }
        .param { color: #666; }
    </style>
</head>
<body>
    <h2>🧮 PHP 计算器测试</h2>
    <div class="result">
        <h3>计算结果:</h3>
        <p>操作: <?php echo $op_name; ?> (<?php echo htmlspecialchars($op); ?>)</p>
        <p>参数 a: <span class="param"><?php echo $a; ?></span></p>
        <p>参数 b: <span class="param"><?php echo $b; ?></span></p>
        <p><strong>结果: <?php echo $result; ?></strong></p>
    </div>

    <h3>测试示例（请使用编码后的 + ）:</h3>
    <ul>
        <li><a href="?a=10&b=5&op=%2B">10 + 5 = ?</a></li>
        <li><a href="?a=10&b=5&op=-">10 - 5 = ?</a></li>
        <li><a href="?a=10&b=5&op=*">10 × 5 = ?</a></li>
        <li><a href="?a=10&b=5&op=/">10 ÷ 5 = ?</a></li>
    </ul>

    <p><small>URL示例: a.php?a=10&b=5&op=%2B</small></p>
</body>
</html>