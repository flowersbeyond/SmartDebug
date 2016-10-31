Note: FixPattern的形式为“A->B”，表示A修改为B

/**
Structure: while( Expression ) Statement

Author: 李天池
Update: 2015-10-27 22:14:45

*/

List<FixPattern> generate( Statement whileStatement){
	E = whileStatement.getExpression();
	S = whileStatement.getStatement();
	
	List<FixPattern> result = {};
	
	changeE = fixExpression(E);
	
	
}


List <FixPattern> generate(Expression E){
	List <FixPattern> result = {};
	
	//类型匹配的变量替换（处理 诸如 i 错写成 j 的场景）
V_e = getVariables(E);
	For(v in V_e){
		A_ve = findAccess(v);
		For(v’ in A_ve){
			If(compatible(v’, v)){
				Result.add(v->v’);
			}
		}
	}
	
	//类型匹配的整型常量替换
	C_e = getConstInt(E);
	For(c in C_e){
		Result.add(c->c + 1, c->c – 1);
	}
	
	//类型匹配的字符串替换
	C_e = getConstString(E);
	For(c in C_e){
		A_ce = findAccess(c);
			For(c’ in A_ce)
				Result.add(c -> c’);
	}

	// 修改调用的方法名 // 处理 1.写错方法名 或 2.因为方法参数传错了而调用了不正确的重载方法 的场景
	Call_e = findMethodCalls(E);
	For (m: call_e) {
		Neighbor_m = findNeighbours(Call_e);
		For( m’ : neighbor_m)
			Result.add (m -> m’)
	}

	// 增加Null Checker
	Fa_e = findAccessedFields(E);
	For(fa: Fa_e) {
		result.add(
			fa
			->
			if(obj!=null){fa;}
		)
	}

// 强转安全检查
	Cast_e = findCastedFields(E);
	For( c: cast_e){
		result.add{
			(T)obj
			->
			if(obj instanceof T){(T)obj}
		}
		//这个形式显然是不合理的 TODO
	}
	
	return V(E), result;
}



List<FixPattern> generate(VariableSet V, Statement S){
	List<FixPattern> result = {};

	// 通过 控制流图/数据流图, 可以得知 执行一条语句会影响到哪些变量, 因而可以:
	foreach 变量 v in V{
		找到 S 中对 v 有影响的语句集合, 记作 S(v);
		foreach 语句 s in S(V){
			// 按照 10.22 学姐的分类, 语句分为 赋值型(形如"变量=expression") 和 非赋值型(形如"expression")
			result.append( 寻找修改Expression的方式(s中的expression) );
		}
	}
	return result;
}

