package com.Hsia.sharding.parser;

import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.Token;

import junit.framework.TestCase;

public class SqlLexerTest extends TestCase{
	

	public void test_lexer() throws Exception {
		String sql = "SELECT * FROM t_sharding WHERE F1 = 1000 where f2 = 1500 ORDER BY F2";
		Lexer lexer = new Lexer(sql);
		for (;;) {
			lexer.nextToken();
			Token tok = lexer.token();
			if (tok == Token.IDENTIFIER) {
				System.out.println(tok.name() + "\t\t" + lexer.stringVal());
			} else if (tok == Token.LITERAL_INT) {
				System.out.println(tok.name() + "\t\t" + lexer.numberString());
			} else {
				System.out.println(tok.name() + "\t\t\t" + tok.name);
			}

			if (tok == Token.WHERE) {
				System.out.println("where pos : " + lexer.pos());
			}

			if (tok == Token.EOF) {
				break;
			}
		}
	}

	public void test_lexer2() throws Exception {
		String sql = "SELECT substr('''a''bc',0,3) FROM dual";
		Lexer lexer = new Lexer(sql);
		
		for (;;) {
			lexer.nextToken();
			Token token = lexer.token();

			if (token == Token.IDENTIFIER) {
				System.out.println(token.name() + "\t\t" + lexer.stringVal());
			} else if (token == Token.LITERAL_INT) {
				System.out.println(token.name() + "\t\t" + lexer.numberString());
			} else if (token == Token.LITERAL_CHARS) {
				System.out.println(token.name() + "\t\t" + lexer.stringVal());
			} else {
				System.out.println(token.name() + "\t\t\t" + token.name);
			}

			if (token == Token.EOF) {
				break;
			}
		}
	}
}