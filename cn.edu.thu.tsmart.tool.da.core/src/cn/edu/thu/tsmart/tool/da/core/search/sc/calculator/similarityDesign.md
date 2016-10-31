# Expression 相似度设计
---

# 0. Background, Motivation and Requirement


## 背景

存在一个宏大叙事: Java 程序 Debug 辅助工具. 

工具要达到的最终效果是: 

用户在 Java 代码的某一行设置一个类似断点的东西, 然后写一个布尔表达式 p (譬如 a==42, 其中 a 是在这一行可访问到的标识符).

然后工具检查: 当代码某一次 ("断点" 触发条件也可以由用户设定) 执行到这一行的时候, p 是否为真. 

若 p 不为真, 则工具尝试修改 Java 代码, 使得修改完重新执行时, 当某一次执行到这一行时 p 为真.

Fault Localization 技术能够知道 "改哪儿". 我们现阶段的工作是: 构造能得到 "改成啥" 的算法.

算法的一部分是: 将 Java 程序中的一个 Expression 替换成另一个 Expression.

## Expression

从语言 [specification](https://en.wikipedia.org/wiki/Programming_language_specification) (或[这里](http://tieba.baidu.com/p/3830422437)) 的角度看, [Expression](https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html) 是 Java 的一个语法结构. 

从语言[实现](https://en.wikipedia.org/wiki/Programming_language_implementation) (或[这里](https://book.douban.com/subject/10482195/)) 角度看, JDT Core 库实现了一组表达 Java 语法结构的数据结构, 即以 [ASTNode](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FASTNode.html) 作为父类的一系列类, 来表达 Java 的 AST. 其中就有 [Expression](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FExpression.html) 类, 它实现了 Java 的 Expression 语法结构.

([这里](http://blog.csdn.net/lovelion/article/details/19050155)是使用 JDT Core 库的一个不错的中文入门教程.)

## 目的, 需求

啥是 Expression 相似度, 又为啥要计算 Expression 相似度?

已经有这两个模块, 分别能够: 

1. 输入一个要被替换掉的 Expression (以及必要的上下文), 输出一个可能作为替代品的 Expression 集合 (这个集合里的 Expression 不妨称为 Candidate Expression). (powered by [codehint](http://jgalenson.github.io/codehint/))

2. 验证将 Expression e1 替换为 Expression e2 后, 上文 "背景" 中提到的需求 p 是否被满足.

直接的做法是, 用模块 1 产生的每个 Candidate Expression e2 一一尝试替换原来的 Expression e1, 然后一一用模块 2 验证.

然而验证的时间成本很大. 而我们只希望找到一个可行解, 而非全部解.

所以, 如果能对 Candidate Expression 们恰当地排序, 让更可能正确的 Candidate Expression 先做验证, 就可能提高效率.
<!--
夫矩阵者, 复杂也; 特征值者, 简洁也.

夫 (Expression, Expression) pair, 复杂也. Similarity 者, 简洁也. -->

为了排序, 需要给 Candidate Expression 集赋予某种[全序关系](https://zh.wikipedia.org/zh-cn/全序关系). 理想地, 这种全序关系应该有这样的性质: 

+ 针对一个要被替换的 Expression e1, 有两个 Candidate Expression e2, e3. e2 < e3 当且仅当 "用 e2 替换 e1" 比 "用 e3 替换 e1" 更可能正确.

可以看出, e2 和 e3 的排序依赖于 e1. 于是, 出于朴素的 "特征值" 思想, 我这样设计: 把每个 (Expression, Candidate Expression) pair (在大的工具的设计中, 这被称为一个 fix) 映射到一个实数, 称为相似度, 记作 Similarity(·,·). 

并基于相似度这样定义 Candidate Expression 的序: 针对一个要被替换的 Expression e1, e2 < e3 当且仅当 Similarity(e1, e2) <= Similarity(e1, e3).

所以, 对 Similarity 的定义提出了这样的要求: 用 e2 替换 e1 越可能正确, Similarity(e1, e2) 越大.

# 1. 正文

在进一步的设计中, 我明确了:

+ 正规性: Similarity(e1, e2) ∈ [0,1], 为了处理方便.
+ 无对称性: Similarity(e1, e2) 不一定等于 Similarity(e2, e1). 因为改错方式往往不是可逆的.

## TODO 困了

基础函数是 字符串相似度, 数相似度, 类型相容度

Expression 成分各异, 但经常可以递归拆开结构, 于是 Similarity 计算函数经常可以递归调用

有类型 用上类型信息; 无类型信息时 fallback 回无类型的计算方案

不考虑 context (往往也考虑不了), 只贪心地考虑 Expression 这个点, 它的内部


## 谜之设计

### valueTypeVector

2015-12-31 15:56:59

除了 ArrayInitializer, ThisExpression, TypeLiteral, VariableDeclarationExpression 这四个奇葩, 其他 (我们处理范围内的) Expression 都有一定的 "可能的值类型".

值类型粗略分为 8 类:

整数, 浮点数, 布尔, 字符, String, 其他referenced(即非基本类型)对象, null, void

然后, 譬如 PostfixExpression 的值可能是 整数, 浮点数, 字符. 那么 PostfixExpression 的 valueTypeVector 就定义为 (1,1,0,1,0,0,0,0).

对四奇葩之外的每个 Expression 类型, 都能按照其可能的值类型找出 valueTypeVector.

这样, 在计算 Similarity(e1, e2) 时, 若不需要关心 e1 和 e2 的结构细节, 可以用 e1 和 e2 两者 Expression 的 valueTypeVector 算一个向量间的距离, 然后经过处理, 作为 e1 和 e2 的相似度. 

各种 Expression 的 valueTypeVector 两两之间的距离可以预先算好.

## 计算 Similarity 的代码写法

对外提供: SimilarityCalculator.calculateSimilarity(Expression e1, Expression e2)

内部计算入口: SimilarityCalculator.sim(Expression e1, Expression e2)

SimilarityCalculator.sim 中, 根据 e1 类型, 调用不同 XXXCalculator.sim((XXX)e1, e2). XXX = {ArrayAccess,..., VariableDeclarationExpression}

XXXCalculator.sim 中, 判断 e2 类型. 若 (e1,e2) 类型组合较特殊, 则特别做计算; 否则使用根据 valueTypeVector 预先算好的值.

现在的目标: 

	else {
		return SimilarityConstant.sim(e1.getNodeType(), e2.getNodeType());
	}