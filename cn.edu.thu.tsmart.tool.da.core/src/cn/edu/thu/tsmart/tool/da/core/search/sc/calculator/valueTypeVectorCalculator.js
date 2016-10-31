var raw=
{
	"ASTNode.ARRAY_ACCESS":[1,1,1,1,1,1,1,0],
	"ASTNode.ARRAY_CREATION":[0,0,0,0,0,1,0,0],
	"ASTNode.ASSIGNMENT":[1,1,1,1,1,1,1,0],
	"ASTNode.BOOLEAN_LITERAL":[0,0,1,0,0,0,0,0],
	"ASTNode.CAST_EXPRESSION":[1,1,1,1,1,1,0,0],
	"ASTNode.CHARACTER_LITERAL":[1,0,0,1,1,0,0,0],
	"ASTNode.CLASS_INSTANCE_CREATION":[0,0,1,0,0,0,0,0],
	"ASTNode.CONDITIONAL_EXPRESSION":[1,1,1,1,1,1,1,0],
	"ASTNode.FIELD_ACCESS":[1,1,1,1,1,1,1,0],
	"ASTNode.INFIX_EXPRESSION":[1,1,1,0,1,0,0,0],
	"ASTNode.INSTANCEOF_EXPRESSION":[0,0,1,0,0,0,0,0],
	"ASTNode.METHOD_INVOCATION":[1,1,1,1,1,1,1,1],
	"ASTNode.NULL_LITERAL":[0,0,0,0,0,0,1,0],
	"ASTNode.NUMBER_LITERAL":[1,1,0,1,0,0,0,0],
	"ASTNode.PARENTHESIZED_EXPRESSION":[1,1,1,1,1,1,1,0],
	"ASTNode.POSTFIX_EXPRESSION":[1,1,0,1,0,0,0,0],
	"ASTNode.PREFIX_EXPRESSION":[1,1,1,1,0,0,0,0],
	"ASTNode.STRING_LITERAL":[0,0,0,0,1,0,0,0],
	"ASTNode.SUPER_FIELD_ACCESS":[1,1,1,1,1,1,1,0],
	"ASTNode.SUPER_METHOD_INVOCATION":[1,1,1,1,1,1,1,1]
};

/* 
目标:
if(type1 == ASTNode.ARRAY_ACCESS){
	if(type2 == ASTNode.ARRAY_ACCESS) return 1.0;
	if(type2 == ASTNode.ARRAY_CREATION) return 0.125;
	...
} 
...
if(type1 == ASTNode.SUPER_METHOD_INVOCATION){
	...
	if(type2 == ASTNode.SUPER_FIELD_ACCESS) return 0.875;
	if(type2 == ASTNode.SUPER_METHOD_INVOCATION) return 1.0;
} 
*/
function calc(k1, k2){
	var n=raw[k1].length;
	var same=0;
	for(var i=0;i<n;i++){
		if(raw[k1][i]==raw[k2][i]) same++;
	}
	return same/n;
}

for(k1 in raw){
	console.log('if(type1 == '+k1+'){')
	for(k2 in raw){
		console.log('\tif(type2 == '+k2+') return '+calc(k1,k2)+';');
	}
	console.log('}')
}