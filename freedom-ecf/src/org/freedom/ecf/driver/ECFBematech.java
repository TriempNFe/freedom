
package org.freedom.ecf.driver;

import static org.freedom.ecf.driver.EStatus.*;
import static org.freedom.ecf.driver.EStatus.RETORNO_INDEFINIDO;
import static org.freedom.ecf.driver.EStatus.RETORNO_OK;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.freedom.ecf.com.Serial;

/**
 * Classe implementa metodos de acesso a comandos de impress�o <BR>
 * Projeto: freedom-ecf <BR>
 * Pacote: org.freedom.ecf.driver <BR>
 * Classe:
 * 
 * @(#)ECFBematech.java <BR>
 *                      <BR>
 *                      Este programa � licenciado de acordo com a LGPL (Lesser General Public License), <BR>
 *                      vers�o 2.1, Fevereiro de 1999 <BR>
 *                      A LGPL deve acompanhar todas PUBLICA��ES, DISTRIBUI��ES e REPRODU��ES deste Programa. <BR>
 *                      Caso uma c�pia da LGPL n�o esteja dispon�vel junto com este Programa, voc� pode contatar <BR>
 *                      o LICENCIADOR ou ent�o pegar uma c�pia em: <a href=http://creativecommons.org/licenses/LGPL/2.1/legalcode.pt> Creative Commons</a> <BR>
 *                      Para poder USAR, PUBLICAR, DISTRIBUIR, REPRODUZIR ou ALTERAR este Programa � preciso estar de acordo com os termos da LGPL. <BR>
 *                      <BR>
 * @author Setpoint Inform�tica Ltda. Robson Sanchez/Alex Rodrigues <BR>
 * @version 1.0.0 - 05/04/2006 <BR>
 *          <BR>
 */

public class ECFBematech extends AbstractECFDriver {

	/**
	 * Construtor da classe ECFBematech. <BR>
	 */
	public ECFBematech() {

		super();
	}

	/**
	 * Construtor da classe ECFBematech. <BR>
	 * Inicia a constru��o da classe chamando o construtor padr�o da classe super <BR>
	 * e chama o metodo ativaPorta(int). <BR>
	 * 
	 * @param com
	 *            parametro para ativa��o da porta serial.<BR>
	 */
	public ECFBematech( final int com ) {

		super();
		ativaPorta( com );
	}

	/**
	 * Construtor da classe ECFBematech. <BR>
	 * Inicia a constru��o da classe chamando o construtor padr�o da classe super <BR>
	 * e chama o metodo ativaPorta(int). <BR>
	 * 
	 * @param port
	 *            parametro para ativa��o da porta serial.<BR>
	 */
	public ECFBematech( final String port ) {

		super();
		ativaPorta( Serial.convPorta( port ) );
	}

	/**
	 * Prepara o comando conforme o protocolo de comunica��o com a impressora. <BR>
	 * O protocolo de comunica��o com a impressora � estruturado <BR>
	 * em blocos e possui a seguinte forma: <BR>
	 * <BR>
	 * STX - byte indicativo de inicio de transmiss�o. <BR>
	 * NBL -byte nenos significativo, da soma do n�mero de bytes que ser�o enviados. <BR>
	 * NBH - byte mais significativo, da soma do n�mero de bytes que ser�o enviados. <BR>
	 * CMD - Sequ�ncia de bytes que comp�e o comando e seus par�metros. <BR>
	 * CSL - byte menos significativo, da soma dos valores dos bytes que comp�em o camando e seu par�metros(CMD). <BR>
	 * 
	 * @param CMD
	 *            comando a ser executado e seus par�metros. <BR>
	 * @see org.freedom.ecf.driver.AbstractECFDriver#preparaCmd(byte[])
	 */
	public byte[] preparaCmd( final byte[] CMD ) {

		final int tamCMD = CMD.length;
		final int tam = tamCMD + 2;
		int soma = 0;
		byte CSL = 0;
		byte CSH = 0;
		final byte NBL = (byte) ( tam % 256 );
		final byte NBH = (byte) ( tam / 256 );
		byte[] retorno = new byte[ 5 + tamCMD ];

		retorno[ 0 ] = STX;
		retorno[ 1 ] = NBL;
		retorno[ 2 ] = NBH;

		for ( int i = 0; i < tamCMD; i++ ) {
			soma += CMD[ i ];
			retorno[ i + 3 ] = CMD[ i ];
		}

		CSL = (byte) ( soma % 256 );
		CSH = (byte) ( soma / 256 );

		retorno[ retorno.length - 2 ] = CSL;
		retorno[ retorno.length - 1 ] = CSH;

		return retorno;
	}

	/**
	 * Este metodo executa o comando chamando os metodos <BR>
	 * preparaCmd(byte[]) <BR>
	 * enviaCmd(byte[]) <BR>
	 * aguardaImpressal(byte[]) <BR>
	 * e devolve o resultado do emvio do camando. <BR>
	 * 
	 * @param CMD
	 *            comando a ser executado e seus par�metros. <BR>
	 * @see org.freedom.ecf.driver.AbstractECFDriver#executaCmd(byte[], int)
	 */
	public int executaCmd( final byte[] CMD, final int tamRetorno ) {

		byte[] retorno = null;
		byte[] cmd = null;

		cmd = preparaCmd( CMD );
		retorno = enviaCmd( cmd, tamRetorno );

		return checkRetorno( retorno );
	}

	/**
	 * Converte o retorno dos comandos do formato BCD para ASCII. <BR>
	 * 
	 * @param bcdParam
	 *            Retorno a ser convertido. <BR>
	 * @return String com o resultado da conver��o. <BR>
	 */
	private String bcdToAsc( final byte[] bcdParam ) {

		final StringBuffer retorno = new StringBuffer();

		int bcd = 0;
		byte byteBH = 0;
		byte byteBL = 0;

		for ( int i = 0; i < bcdParam.length; i++ ) {

			bcd = bcdParam[ i ];

			// Ajuste dos bytes para o padr�o de c�lculo (o java trabalha com bytes de -128 a 127)
			if ( bcd < 0 ) {
				bcd += 256;
			}

			byteBH = (byte) ( bcd / 16 );
			byteBL = (byte) ( bcd % 16 );

			retorno.append( byteBH );
			retorno.append( byteBL );
		}

		return retorno.toString();
	}

	/**
	 * Formata os retorna enviado pela impressora <BR>
	 * separando o STATUS do estado da impressora dos dados do retorno, onde <BR>
	 * ACK (06) - byte indicativo de recebimento correto. <BR>
	 * ST1 2 ST2 - bytes de estado da impressora. <BR>
	 * NAK (15h ou 21d) - byte indicativo de recebimento incorreto. <BR>
	 * <BR>
	 * O retorno tem a seguinte sintaxe : <BR>
	 * [ACK][retorno solicitado][ST1][ST2] <BR>
	 * 
	 * @param bytes
	 *            bytes retornados pela porta serial.<BR>
	 * @return retorno indece para a mensagem.
	 * @see org.freedom.ecf.driver.AbstractECFDriver#checkRetorno(byte[])
	 */
	public int checkRetorno( final byte[] bytes ) {

		int retorno = 0;
		byte ack = 0;
		byte st1 = 0;
		byte st2 = 0;

		if ( bytes != null ) {

			ack = bytes[ 0 ];

			if ( bytes.length > 3 ) {

				st1 = bytes[ bytes.length - 2 ];
				st2 = bytes[ bytes.length - 1 ];

				final byte[] bytesLidos = new byte[ bytes.length - 3 ];
				System.arraycopy( bytes, 1, bytesLidos, 0, bytesLidos.length );
				setBytesLidos( bytesLidos );
			}

			if ( ack == ACK && st1 == 0 && st2 == 0 ) {
				retorno = 1;
			} else {
				retorno = -27; // Status da impressora diferente de 6,0,0 (ACK, ST1 e ST2)
				retorno = checkST1( st1 );
				retorno = checkST2( st2 );
			}
		}

		return retorno;
	}

	/**
	 * Auxilia o metodo checkRetorno.<BR>
	 * 
	 * @param ST1
	 * @return retorno checado
	 */
	private int checkST1( final byte ST1 ) {

		int retorno = 0;
		byte st1 = ST1;

		if ( st1 > 127 ) {
			st1 -= 128;
		}
		if ( st1 > 63 ) {
			st1 -= 64;
		}
		if ( st1 > 31 ) {
			st1 -= 32;
		}
		if ( st1 > 15 ) {
			st1 -= 16;
		}
		if ( st1 > 7 ) {
			st1 -= 8;
		}
		if ( st1 > 3 ) {
			st1 -= 4;
		}
		if ( st1 > 1 ) {
			st1 -= 2;
		}
		if ( st1 > 0 ) {
			st1 -= 1;
			retorno = -2; // "Par�metro inv�lido na fun��o. ou N�mero de par�metros inv�lido na fun�ao"
		}

		return retorno;
	}

	/**
	 * Auxilia o metodo checkRetorno.<BR>
	 * 
	 * @param ST2
	 * @return retorno checado
	 */
	private int checkST2( final byte ST2 ) {

		int retorno = 0;
		byte st2 = ST2;

		if ( st2 > 127 ) {
			retorno = -2;
			st2 -= 128;
		}
		if ( st2 > 63 ) {
			st2 -= 64;
		}
		if ( st2 > 31 ) {
			st2 -= 32;
		}
		if ( st2 > 15 ) {
			st2 -= 16;
		}
		if ( st2 > 7 ) {
			st2 -= 8;
		}
		if ( st2 > 3 ) {
			st2 -= 4;
		}
		if ( st2 > 1 ) {
			st2 -= 2;
		}
		if ( st2 > 0 ) {
			st2 -= 1;
			retorno = -2;
		}

		return retorno;
	}
	
	public EStatus decodeReturnECF( final int arg ) {

		EStatus status = RETORNO_OK;

		switch ( arg ) {

			case 0 :
				status = BEMA_ERRO_COMUNICACAO;
				break;
			case 1 :
				status = RETORNO_OK;
				break;
			case -2 :
				status = BEMA_PARAMETRO_INVALIDO;
				break;
			case -3 :
				status = BEMA_ALIQUOTA_NAO_PROGRAMADA;
				break;
			case -4 :
				status = BEMA_ARQ_INI_NAO_ENCONTRADO;
				break;
			case -5 :
				status = BEMA_ERRO_ABRIR_PORTA;
				break;
			case -8 :
				status = BEMA_ERRO_GRAVAR_RETORNO;
				break;
			case -27 :
				status = BEMA_NAO_STATUS_600;
				break;
			case -30 :
				status = BEMA_FUNCAO_NAO_COMPATIVEL;
				break;
			case -31 :
				status = BEMA_FORMA_PAGAMENTO_NAO_FINALIZADA;
				break;
			default :
				EStatus stmp = RETORNO_INDEFINIDO;
				stmp.setMessage( "Retorno indefinido: " + arg );
				status = stmp;
				break;
		}
		
		return status;
	}

	/**
	 * Atrav�s do comando 01, pode-se alterar o s�mbolo da moeda usando como tamanho de par�metro<br>
	 * dois caracteres ASCII alfanum�ricos. Ex: �" R" (um espa�o em branco e a letra R mai�scula).<br>
	 * O s�mbolo monet�rio �$� j� est� programado, sendo assim, n�o precisa ser inserido.<br>
	 * 
	 * @param simbolo
	 *            simboleo da moeda.<br>
	 * @return estado da impressora.<br>
	 */
	public int alteraSimboloMoeda( final String simbolo ) {

		byte[] CMD = { ESC, 1 };
		
		final int tamanho = 2;
		String tmp = simbolo.trim();
		
		if ( tamanho < tmp.length() ) {
			tmp = tmp.substring( 0, tamanho );
		} else {
			tmp = replicate( " ", tamanho - tmp.length() );
			tmp += simbolo;
		}
		
		CMD = adicBytes( CMD, tmp.getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Poder� ser adicionado at� 16 al�quotas tribut�rias.<br>
	 * Sempre verifique se j� exite al�quotas programadas, pois<br>
	 * n�o � permitido alterar as al�quotas que j� existem, nem remov�-las, apenas adicion�-las.<br>
	 * 
	 * @param aliq
	 *            percentual da al�quota.<br>
	 * @param opt
	 *            indica op��o da al�quota, se � ICMS OU ISS.<br>
	 * @see org.freedom.ecf.driver.AbstractECFDriver#ISS
	 * @see org.freedom.ecf.driver.AbstractECFDriver#ICMS
	 * @return estado da impressora.<br>
	 */
	public int adicaoDeAliquotaTriburaria( final String aliq, final char opt ) {

		byte[] CMD = { ESC, 7 };

		final StringBuffer buf = new StringBuffer();
		
		buf.append( parseParam( aliq, 4, false ) );

		if ( ISS == opt ) {
			buf.append( opt );
		}

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando deve ser utilizado ap�s uma Redu��o Z.<br>
	 * Para entrar no Hor�rio de Ver�o, simplesmente envie este comando � impressora.<br>
	 * Para sair d o Hor�rio de Ver�o, voc� dever� esperar pelo menos 1 hora e em seguida enviar o comando.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaHorarioVerao() {

		final byte[] CMD = { ESC, 18 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Voc� poder� nomear at� 50 (01 at� 50) totalizadores n�o sujeitos para recebimentos,<br>
	 * como no exemplo abaixo, est� sendo nomeado ao primeiro totalizador (01) a "Conta de Luz".<br>
	 * Estes totalizadores devem ser nomeados somente ap�s uma Redu��o Z.<br>
	 * 
	 * @param indice
	 *            indica a posi��o do totalizador.<br>
	 * @param desc
	 *            descri��o do totalizador.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int nomeiaTotalizadorNaoSujeitoICMS( final int indice, final String desc ) {

		byte[] CMD = { ESC, 40 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( indice, 2 ) );
		buf.append( parseParam( desc, 19, false ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando define o tratamento de casas decimais.<br>
	 * Como default, o formato � truncar. Caso queira arredondamento,<br>
	 * utilize como par�metro um n�mero �mpar.<br>
	 * 
	 * Exemplo:<br>
	 * <br>
	 * <p style="background=#eeffee"> // para definir Arredondamento.<br>
	 * programaTruncamentoArredondamento( '1' );<br>
	 * </p>
	 * <br>
	 * 
	 * @param opt
	 *            defini��o de truncamento/arredondamento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaTruncamentoArredondamento( final char opt ) {

		byte[] CMD = { ESC, 39 };

		CMD = adicBytes( CMD, parseParam( opt, 1 ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Voc� poder� programar espa�os entre as linhas do Cupom (em dots) atrav�s deste comando.<br>
	 * Uma linha � igual a 8 dots.<br>
	 * Este comando s� ser� executado caso n�o tenha havido movimenta��o no dia, ou ap�s a Redu��o Z.<br>
	 * Este comando s� est� dispon�vel para a impressora MP-40 FI II.<br>
	 * 
	 * @param espaco
	 *            dots de espa�amento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programarEspacoEntreLinhas( final int espaco ) {

		byte[] CMD = { ESC, 60 };

		CMD = adicBytes( CMD, parseParam( (char) espaco ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Voc� poder� programar o n�mero de linhas entre os cupons.<br>
	 * Este comando deve ser executado no in�cio das opera��es,<br>
	 * sendo que possibilita a impress�o de um Relat�rio Ger�ncial ou de um Comprovante N�o Fiscal,<br>
	 * logo ap�s a impress�o do Cupom Fiscal, sem espa�os em branco.<br>
	 * 
	 * @param espaco
	 *            n�mero de linhas.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programarLinhasEntreCupons( final int espaco ) {

		byte[] CMD = { ESC, 61 };

		CMD = adicBytes( CMD, parseParam( (char) espaco ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * O Departamento s� ser� nomeado, caso n�o tenha havido movimenta��o no dia ou logo ap�s uma Redu��o Z.<br>
	 * Voc� poder� nomear at� 20 Departamentos.<br>
	 * O �ndice 01 � �Geral� e j� vem programado na impressora. O tamanho do par�metro � de 10 bytes.<br>
	 * Este Departamento tem por finalidade armazenar, no dia, a quantidade e o valor de uma<br>
	 * determinada venda, exemplo: departamento Vestu�rio (tudo que foi vendido de cal�as, camisas,<br>
	 * blusas e etc), departamento Gasolina (tudo que foi vendido em gasolina) e etc.<br>
	 * 
	 * @param index
	 *            indicador do departamento.<br>
	 * @param descricao
	 *            descri��o do departamento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int nomeiaDepartamento( final int index, final String descricao ) {

		byte[] CMD = { ESC, 65 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( index, 2 ) );
		buf.append( parseParam( descricao, 20, false ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Abertura de Cupom Fiscal. <br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int aberturaDeCupom() {

		final byte[] CMD = { ESC, 0 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Abertura de Cupom Fiscal, informando o CNPJ/CPF do cliente.<br>
	 * 
	 * @param cnpj
	 *            CNPJ/CPF do cliente.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int aberturaDeCupom( final String cnpj ) {

		byte[] CMD = { ESC, 0 };
		CMD = adicBytes( CMD, parseParam( cnpj, 29, false ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Programa na mem�ria da impressora a unidade de medida que deseja usar no pr�ximo comando<br>
	 * de Venda de Item. Este comando tem validade somente para a impress�o de um Item,<br>
	 * voltando ao default que � sem a unidade de medida.<br>
	 * � necess�rio program�-la, novamente, caso deseje us�-la para a pr�xima venda.<br>
	 * No exemplo abaixo, est� sendo programada a unidade Kg.<br>
	 * 
	 * Exemplo:<br>
	 * <br>
	 * <p style="background=#000000"> // para definir a unidade de medida para o pr�ximo item.<br>
	 * programaUnidadeMedida( "Kg" );// Kilograma<br>
	 * </p>
	 * <br>
	 * 
	 * @param descUnid
	 *            unidade de medida.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaUnidadeMedida( final String descUnid ) {

		byte[] CMD = { ESC, 62, 51 };

		CMD = adicBytes( CMD, parseParam( descUnid, 2, false ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * O pr�ximo comando de Venda de Item imprimir� a Descri��o com este tamanho.<br>
	 * Este comando tem validade somente para a impress�o de um Item,<br>
	 * voltando ao padr�o que � de 29 caracteres.<br>
	 * 
	 * @param descricao
	 *            descri��o do item.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int aumentaDescItem( final String descricao ) {

		byte[] CMD = { ESC, 62, 52 };

		CMD = adicBytes( CMD, parseParam( descricao, 200 ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa a venda de item utilizando valor com duas casas decimais.<br>
	 * Para execu��o deste comando obrigatoriamente o cupom deve estar aberto.<br>
	 * 
	 * @param codProd
	 *            c�digo do produto.<br>
	 * @param descProd
	 *            descri��o do produto.<br>
	 * @param aliquota
	 *            aliquota do item.<br>
	 * @param tpqtd
	 *            tipo de quantidade, inteiro ou decimal.<br>
	 * @param qtd
	 *            quantidade do item.<br>
	 * @param valor
	 *            valor do item.<br>
	 * @param tpdesc
	 *            tipo do desconto, percentual ou valor.<br>
	 * @param desconto
	 *            valor do desconto.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int vendaItem( final String codProd, final String descProd, final String aliquota, final char tpqtd, final float qtd, final float valor, final char tpdesc, final float desconto ) {

		byte[] CMD = { ESC, 9 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( codProd, 13 ) );
		buf.append( parseParam( descProd, 29 ) );
		buf.append( parseParam( aliquota, 2 ) );
		if ( tpqtd == QTD_DECIMAL ) {
			buf.append( parseParam( qtd, 7, 3 ) );
		}
		else if ( tpqtd == QTD_INTEIRO ) {
			buf.append( parseParam( qtd, 4, 0 ) );
		}
		buf.append( parseParam( valor, 8, 2 ) );
		if ( tpdesc == DESCONTO_VALOR ) {
			buf.append( parseParam( desconto, 8, 2 ) );
		}
		else if ( tpdesc == DESCONTO_PERC ) {
			buf.append( parseParam( desconto, 4, 0 ) );
		}

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa a venda de item utilizando valor com tr�s casas decimais.<br>
	 * Para execu��o deste comando obrigatoriamente o cupom deve estar aberto.<br>
	 * 
	 * @param codProd
	 *            c�digo do produto.<br>
	 * @param descProd
	 *            descri��o do produto.<br>
	 * @param aliquota
	 *            aliquota do item.<br>
	 * @param tpqtd
	 *            tipo de quantidade, inteiro ou decimal.<br>
	 * @param qtd
	 *            quantidade do item.<br>
	 * @param valor
	 *            valor do item.<br>
	 * @param tpdesc
	 *            tipo do desconto, percentual ou valor.<br>
	 * @param desconto
	 *            valor do desconto.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int vendaItemTresCasas( final String codProd, final String descProd, final String aliquota, final char tpqtd, final float qtd, final float valor, final char tpdesc, final float desconto ) {

		byte[] CMD = { ESC, 56 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( codProd, 13 ) );
		buf.append( parseParam( descProd, 29 ) );
		buf.append( parseParam( aliquota, 2 ) );
		if ( tpqtd == QTD_DECIMAL ) {
			buf.append( parseParam( qtd, 7, 3 ) );
		}
		else if ( tpqtd == QTD_INTEIRO ) {
			buf.append( parseParam( qtd, 4, 0 ) );
		}
		
		buf.append( parseParam( valor, 8, 3 ) );
		
		if ( tpdesc == DESCONTO_VALOR ) {
			buf.append( parseParam( desconto, 8, 2 ) );
		}
		else if ( tpdesc == DESCONTO_PERC ) {
			buf.append( parseParam( desconto, 4, 0 ) );
		}

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa a venda de item utilizando valor com tr�s casas decimais.<br>
	 * Para execu��o deste comando obrigatoriamente o cupom deve estar aberto.<br>
	 * 
	 * @param sitTrib
	 *            situa��o tribut�ria do item.<br>
	 * @param valor
	 *            valor do item.<br>
	 * @param qtd
	 *            quantidade do item.<br>
	 * @param desconto
	 *            valor do desconto no item<br>
	 * @param acrescimo
	 *            valor de acr�cimo no item<br>
	 * @param departamento
	 *            indice do departamento<br>
	 * @param unidade
	 *            unidade de medida do item<br>
	 * @param codProd
	 *            c�digo do produto.<br>
	 * @param descProd
	 *            descri��o do produto.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int vendaItemDepartamento( final String sitTrib, final float valor, final float qtd, final float desconto, final float acrescimo, final int departamento, final String unidade, final String codProd, final String descProd ) {

		byte[] CMD = { ESC, 63 };

		final StringBuffer buf = new StringBuffer( 312 );

		buf.append( parseParam( sitTrib, 2, false ) );
		buf.append( parseParam( valor, 9, 3 ) );
		buf.append( parseParam( qtd, 7, 3 ) );
		buf.append( parseParam( desconto, 10, 2 ) );
		buf.append( parseParam( acrescimo, 10, 2 ) );
		buf.append( parseParam( departamento, 2 ) );
		buf.append( "00000000000000000000" );
		buf.append( parseParam( unidade, 2, false ) );
		buf.append( parseParam( codProd + (char) 0, 49, false ) );
		buf.append( parseParam( descProd + (char) 0, 200, false ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa o cancelamento da venda do ultimo item.<br>
	 * O item neste caso s� poder� ser cancelado ap�s a sua venda.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int cancelaItemAnterior() {

		final byte[] CMD = { ESC, 13 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa o cancelamento da venda do item<br>
	 * indicado pelo indice passado por parametro.<br>
	 * O item neste caso s� poder� ser cancelado ap�s a sua venda.<br>
	 * 
	 * @param item
	 *            indice do item a ser cancelado.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int cancelaItemGenerico( final int item ) {

		byte[] CMD = { ESC, 31 };

		CMD = adicBytes( CMD, parseParam( item, 4 ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Atrav�s deste comando, � dado o in�cio ao fechamento do cupom.<br>
	 * A impressora imprimir� o TOTAL das vendas. Os par�metros que est�o sendo passados<br>
	 * por este exemplo s�o: op��o de Desconto ( "D" ) ou de Acr�cimo ( "A" ) e o Percentual.<br>
	 * 
	 * @param opt
	 *            op��o de Desconto ou Acr�cimo.<br>
	 * @param valor
	 *            Percentual.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int iniciaFechamentoCupom( final char opt, final float valor ) {

		byte[] CMD = { ESC, 32 };

		int tamanho = 14;

		if ( opt == ACRECIMO_PERC || opt == DESCONTO_PERC ) {
			tamanho = 4;
		}

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( opt ) );
		buf.append( parseParam( valor, tamanho, 2 ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Atrav�s deste comando, � informado a Forma de Pagamento<br>
	 * que o cliente usou para o pagamento da conta.<br>
	 * Caso a Forma de Pagamento exceda o valor total do Cupom,<br>
	 * n�o ser�o mais permitidas novas formas.<br>
	 * 
	 * @param indice
	 *            indice da forma de pagamento.<br>
	 * @param valor
	 *            valor.<br>
	 * @param descForma
	 *            descri��o complemetar para a forma de pagamento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int efetuaFormaPagamento( final String indice, final float valor, final String descForma ) {

		byte[] CMD = { ESC, 72 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( indice, 2 ) );
		buf.append( parseParam( valor, 14, 2 ) );
		buf.append( parseParam( descForma, 80, true ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando fecha o Cupom, passando como par�metro a mensagem promocional.<br>
	 * Esta mensagem possui um tamanho m�ximo de 492 caracteres, sendo limitada em at� 8 linhas.<br>
	 * Se n�o houver nenhum item vendido, n�o ser� permitido o fechamento do Cupom.<br>
	 * 
	 * @param mensagem
	 *            menssagem promocional de final de cupom.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int finalizaFechamentoCupom( final String mensagem ) {

		byte[] CMD = { ESC, 34, ESC };
		CMD = adicBytes( CMD, parseParam( mensagem, 492, true ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando estar� habilitado para sua execu��o, em qualquer parte do cupom,<br>
	 * desde que haja pelo menos um item vendido.<br>
	 * S� � permitido o cancelamento do �ltimo cupom fiscal impresso.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int cancelaCupom() {

		final byte[] CMD = { ESC, 14 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * S�o permitidos at� 49 formas de pagamento, no tamanho de 16 caracteres, sendo que a 01 ser�<br>
	 * sempre �Dinheiro� (Default). Este comando poder� ser executado a qualquer hora do dia, retornando<br>
	 * pela porta serial o �ndice da forma programada.<br>
	 * Ap�s a sua totaliza��o na Redu��o Z todas as formas<br>
	 * de pagamento programadas ser�o apagadas da impressora, permanecendo somente a forma 01<br>
	 * (Dinheiro). � programado, apenas, uma forma por vez.<br>
	 * 
	 * @param descricao
	 *            descri��o da forma de pagamento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public String programaFormaPagamento( final String descricao ) {

		byte[] CMD = { ESC, 71 };

		CMD = adicBytes( CMD, parseParam( descricao, 16 ).getBytes() );

		executaCmd( CMD, 4 );

		return new String( getBytesLidos() );
	}

	/**
	 * Este comando permite estornar valores de uma Forma de Pagamento e inserir em<br>
	 * outra Forma de Pagamento.<br>
	 * Observa��es: O valor a ser estornado n�o pode exceder o total da Forma de Pagamento de<br>
	 * Origem. Este comando s� ser� executado se o Cupom Fiscal estiver fechado.<br>
	 * 
	 * @param descOrigem
	 *            descri��o da forma de pagamento de origem.<br>
	 * @param descDestino
	 *            descri��o da forma de pagamento de destino.<br>
	 * @param valor
	 *            valor a estornar.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int estornoFormaPagamento( final String descOrigem, final String descDestino, final float valor ) {

		byte[] CMD = { ESC, 74 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( descOrigem, 16, false ) );
		buf.append( parseParam( descDestino, 16, false ) );
		buf.append( parseParam( valor, 14, 2 ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Executa a Redu��o Z para a data atual da impressora fiscal.<BR>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int reducaoZ() {

		final byte[] CMD = { ESC, 5 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Executa a Leitura X na impressora fiscal.<BR>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int leituraX() {

		final byte[] CMD = { ESC, 6 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Executa a leitura da memoria fiscal entre datas, e pode ser impresso ou capturado na porta serial.<br>
	 * 
	 * @param dataIni
	 *            data de inicio.<br>
	 * @param dataFim
	 *            data limite.<br>
	 * @param tipo
	 *            I para impress�o e R para porta serial.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int leituraMemoriaFiscal( final Date dataIni, final Date dataFim, final char tipo ) {

		byte[] CMD = { ESC, 8 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( dataIni ) );
		buf.append( parseParam( dataFim ) );
		buf.append( tipo );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Executa a leitura da memoria fiscal entre redu��es, e pode ser impresso ou capturado na porta serial.<br>
	 * 
	 * @param ini
	 *            data de inicio.<br>
	 * @param fim
	 *            data limite.<br>
	 * @param tipo
	 *            I para impress�o e R para porta serial.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int leituraMemoriaFiscal( final int ini, final int fim, final char tipo ) {

		byte[] CMD = { ESC, 8 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( ini, 6 ) );
		buf.append( parseParam( fim, 6 ) );
		buf.append( tipo );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Atrav�s deste comando, voc� obtem a Leitura X pela porta Serial.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int leituraXSerial() {

		final byte[] CMD = { ESC, 69 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Neste relat�rio � permitido a impress�o de um texto qualquer, com no m�ximo 620 caracteres<br>
	 * que poder� ser enviado v�rias vezes. Com a execu��o deste comando, a impressora imprimir�, antes,<br>
	 * uma Leitura �X�. Este relat�rio est� limitado a 10 (dez) minutos de dura��o. Se n�o for enviando o<br>
	 * comando para fechar este relat�rio, ap�s 10 minutos, a impressora fechar� automaticamente.<br>
	 * 
	 * @param texto
	 *            texto a ser impresso no relat�rio.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int relatorioGerencial( final String texto ) {

		byte[] CMD = { ESC, 20 };

		CMD = adicBytes( CMD, parseParam( texto, 620, true ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando poder� ser usado para Fechar o Relat�rio Ger�ncial e tamb�m o Comprovante<br>
	 * N�o Fiscal Vinculado.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int fechamentoRelatorioGerencial() {

		final byte[] CMD = { ESC, 21 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comprovante � usado em casos de Suprimento (entrada de dinheiro em caixa, usado<br>
	 * normalmente no in�cio do dia - Abertura de Caixa), Sangrias (retira de dinheiro do caixa) ou para<br>
	 * Recebimentos n�o sujeitos ao ICMS.<br>
	 * 
	 * Obs.: No caso de recebimentos n�o sujeitos a ICMS o totalizador deve estar previamente programado.<br>
	 * 
	 * @param opt
	 *            "SA" para sangria,<br>
	 *            "SU" para suprimento ou<br>
	 *            o indice o totalizador n�o sujeito a ICMS.<br>
	 * @param valor
	 *            valor do comprovante.<br>
	 * @param formaPag
	 *            forma de pagamento.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int comprovanteNFiscalNVinculado( final String opt, final float valor, final String formaPag ) {

		byte[] CMD = { ESC, 25 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( opt, 2 ) );
		buf.append( parseParam( valor, 14, 2 ) );
		buf.append( parseParam( formaPag, 16 ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * S� ser� executado logo ap�s um Cupom Fiscal ou de um Comprovante N�o Fiscal N�o Vinculado<br>
	 * (Recebimentos), al�m disto, a Forma de Pagamento deve ter sido utilizada no �ltimo Cupom. Este<br>
	 * Comprovante � usado para a impress�o do TEF (Transfer�ncia Eletr�nica de Fundo) e tamb�m para<br>
	 * compras � prazo.<br>
	 * � possiv�l tamb�m vincular o comprovante n�o fiscal para um cupom j� impresso atrav�s do COO<br>
	 * (Contado de Ordem de Opera��o).<br>
	 * 
	 * Obs.: N�o utilizado para a forma de pagamento "Dinheiro".<br>
	 * 
	 * @param formaPag
	 *            forma de pagamento.<br>
	 * @param valor
	 *            valor do comprovante.<br>
	 * @param doc
	 *            n�mero de COO.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int abreComprovanteNFiscalVinculado( final String formaPag, final float valor, final int doc ) {

		byte[] CMD = { ESC, 66 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( formaPag, 16, false ) );
		buf.append( parseParam( valor, 14, 2 ) );
		buf.append( parseParam( doc, 6 ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este Comprovante pode ser usado para descrever melhor a Forma de Pagamento passado no<br>
	 * Cupom Fiscal, pode ser passado at� 620 caracteres. Este Comprovante possui 2 minutos de impress�o,<br>
	 * sendo que o comando poder� ser enviado v�rias vezes dentro deste tempo. Ap�s estes 2 minutos o<br>
	 * Comprovante fechar� automaticamente, caso contr�rio dever� ser enviado o comando de Fechamento<br>
	 * do Relat�rio Ger�ncial.<br>
	 * 
	 * @param texto
	 *            texto a ser impresso.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int usaComprovanteNFiscalVinculado( final String texto ) {

		byte[] CMD = { ESC, 67 };

		CMD = adicBytes( CMD, parseParam( texto, 620, false ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando dever� ser executado ap�s um Recebimento N�o Sujeito ao ICMS ou ao t�rmino<br>
	 * de um Cupom Fiscal. A impressora ir� aguardar 5 (cinco) segundos para que seja inserido o documento,<br>
	 * caso contr�rio, a impressora retornar� ao estado normal de opera��o, indicando o �status� de<br>
	 * �comando n�o executado.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int autenticacaoDeDocumento() {

		final byte[] CMD = { ESC, 16 };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Voc� poder� criar um caracter para a autentica��o de seus documentos atrav�s deste comando.<br>
	 * Cada byte � uma coluna, onde o bit menos significativo corresponde � agulha mais alta da<br>
	 * cabe�a de impress�o. Ser� impresso: AUT: logo, data, loja, ECF, COO e o valor.<br>
	 * 
	 * @param caracteres
	 *            caracteres para formata��o da logo.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaCaracterParaAutenticacao( final int[] caracteres ) {

		byte[] CMD = { ESC, 64 };

		byte[] bytes = new byte[ caracteres.length ];
		for ( int i = 0; i < caracteres.length; i++ ) {
			bytes[ i ] = (byte) (caracteres[ i ]-128);
		}
		CMD = adicBytes( CMD, bytes );


		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando executa a abertura da gaveta de dinheiro, no tempo(em milisegundos)<br>
	 * passado pro parametro.<br>
	 * 
	 * @param time
	 *            tempo para abertura da gaveta.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int acionaGavetaDinheiro( final int time ) {

		final byte[] CMD = { ESC, 22, (byte) time };

		return executaCmd( CMD, 3 );
	}

	/**
	 * Verifica o estado da Gaveta atual, se a mesma est� aberta ou fechada. Neste caso,<br>
	 * dever� ler a porta de comunica��o da impressora.<br>
	 * 
	 * @return estado da gaveta de dinheiro.<br>
	 *         00 - Sensor em N�vel Zero.<br>
	 *         FF - Sensor em N�vel Um.<br>
	 */
	public String retornoEstadoGavetaDinheiro() {

		final byte[] CMD = { ESC, 23 };

		executaCmd( CMD, 4 );

		return bcdToAsc( getBytesLidos() );
	}

	/**
	 * Atrav�s deste comando � poss�vel verificar o estado da impressora atual.<br>
	 * 
	 * @see org.freedom.ecf.driver.EStatus
	 * @return estado da impressora.<br>
	 */
	public List<EStatus> getStatus() {

		final byte[] CMD = preparaCmd( new byte[ ] { ESC, 19 } );

		final byte[] ret = enviaCmd( CMD, 3 );

		List<EStatus> returnOfAction = new ArrayList<EStatus>();

		int st1 = ret[ 1 ];
		int st2 = ret[ 2 ];

		if ( st1 >= 128 ) {
			st1 -= 128;
			returnOfAction.add( BEMA_FIM_DE_PAPEL );
		}
		if ( st1 >= 64 ) {
			st1 -= 64;
			returnOfAction.add( BEMA_POUCO_PAPEL );
		}
		if ( st1 >= 32 ) {
			st1 -= 32;
			returnOfAction.add( BEMA_RELOGIO_ERROR );
		}
		if ( st1 >= 16 ) {
			st1 -= 16;
			returnOfAction.add( BEMA_IMPRESSORA_EM_ERRO );
		}
		if ( st1 >= 8 ) {
			st1 -= 8;
			returnOfAction.add( BEMA_NO_ESC );
		}
		if ( st1 >= 4 ) {
			st1 -= 4;
			returnOfAction.add( BEMA_NO_COMMAND );
		}
		if ( st1 >= 2 ) {
			st1 -= 2;
			returnOfAction.add( BEMA_CUPOM_FISCAL_ABERTO );
		}
		if ( st1 >= 1 ) {
			st1 -= 1;
			returnOfAction.add( BEMA_NU_PARAMS_INVALIDO );
		}

		if ( st2 >= 128 ) {
			st2 -= 128;
			returnOfAction.add( BEMA_TP_PARAM_INVALIDO );
		}
		if ( st2 >= 64 ) {
			st2 -= 64;
			returnOfAction.add( BEMA_OUT_OF_MEMORY );
		}
		if ( st2 >= 32 ) {
			st2 -= 32;
			returnOfAction.add( BEMA_MEMORY_ERROR );
		}
		if ( st2 >= 16 ) {
			st2 -= 16;
			returnOfAction.add( BEMA_NO_ALIQUOTA );
		}
		if ( st2 >= 8 ) {
			st2 -= 8;
			returnOfAction.add( BEMA_OUT_OF_ALIQUOTA );
		}
		if ( st2 >= 4 ) {
			st2 -= 4;
			returnOfAction.add( BEMA_NO_ACESESS_CANCELAMENTO );
		}
		if ( st2 >= 2 ) {
			st2 -= 2;
			returnOfAction.add( BEMA_NO_CNPJ_IE );
		}
		if ( st2 >= 1 ) {
			st2 -= 1;
			returnOfAction.add( BEMA_COMMAND_NO_EXECUTE );
		}
		if ( returnOfAction.size() == 0 ) {
			returnOfAction.add( IMPRESSORA_OK );
		}

		return returnOfAction;
	}

	/**
	 * Retorna as al�quotas programadas, atualmente, na Impressora.<br>
	 * 
	 * @return string concatenda com as aliquotas programadas(cada aliquota tem quatro digitos)<br>
	 */
	public String retornoAliquotas() {

		final byte[] CMD = { ESC, 26 };

		executaCmd( CMD, 36 );

		return bcdToAsc( getBytesLidos() ).substring( 1 );
	}

	/**
	 * Atrav�s deste comando s�o retornados, via serial:<br>
	 * <br>
	 * Totalizadores Parciais Tributados : 112 bytes<br>
	 * Isen��o : 7 bytes<br>
	 * N�o Incid�ncia : 7 bytes<br>
	 * Substitui��o : 7 bytes<br>
	 * Totalizadores N�o Sujeitos ao ICMS : 63 bytes<br>
	 * Sangria : 7 bytes<br>
	 * Suprimentos : 7 bytes<br>
	 * Grande Total : 9 bytes<br>
	 * <br>
	 * 
	 * @return totalizadores parciais<br>
	 */
	public String retornoTotalizadoresParciais() {

		final byte[] CMD = { ESC, 27 };

		executaCmd( CMD, 222 );

		final int[] tam = { 224, 14, 14, 14, 126, 14, 14, 18 };
		final String totalizadores = bcdToAsc( getBytesLidos() );
		final StringBuffer retorno = new StringBuffer();
		int index = 0;

		for ( int i = 0; i < tam.length; i++ ) {

			if ( i > 0 ) {
				retorno.append( ',' );
			}

			retorno.append( totalizadores.substring( index, ( index + tam[ i ] ) ) );
			index += tam[ i ];
		}

		return retorno.toString();
	}

	/**
	 * Retorna o subtotal do cupom do �ltimo cupom.<br>
	 * 
	 * @return subtotal<br>
	 */
	public String retornoSubTotal() {

		final byte[] CMD = { ESC, 29 };

		executaCmd( CMD, 10 );
		return bcdToAsc( getBytesLidos() );
	}

	/**
	 * Este comando poder� ser utilizado logo ap�s a abertura de um Cupom Fiscal, assim recebendo<br>
	 * o seu n�mero, ou ap�s qualquer Cupom fechado.<br>
	 * 
	 * @return n�mero do cupom<br>
	 */
	public String retornoNumeroCupom() {

		final byte[] CMD = { ESC, 30 };

		executaCmd( CMD, 6 );

		return bcdToAsc( getBytesLidos() );
	}
	
	public boolean retornoDocumentoAberto() {
		
		List<EStatus> status = getStatus();
		if ( status != null ) {
			for ( EStatus s : status ) {
				if ( s == EStatus.BEMA_CUPOM_FISCAL_ABERTO ) {
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Retorna a imforma��o da impressora comforme o parametro especificado.<br>
	 * 
	 * <table border=1>
	 * <tr>
	 * <td><b>paramtro<b></td>
	 * <td><b>vari�vel<b></td>
	 * <td><b>tamanho<b></td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>n�mero de serie</td>
	 * <td>15 caracteres ASCII</td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td>Vers�o do FIRMWARE</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>CNPJ/IE</td>
	 * <td>33 caracteres ASCII</td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td>Grande Total</td>
	 * <td>9 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td>Cancelamentos</td>
	 * <td>7 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td>Descontos</td>
	 * <td>7 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td>Contador Seq��ncial</td>
	 * <td>3 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td>N�mero de Opera��es N�o Fiscais</td>
	 * <td>3 bytes</td>
	 * <tr>
	 * <tr>
	 * <td>8</td>
	 * <td>N�mero de Cupons Cancelados</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td>N�mero de Redu��es</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>10</td>
	 * <td>N�mero de Interven��es T�cnicas</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>11</td>
	 * <td>N�mero de Interven��es T�cnicas</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>12</td>
	 * <td>N�mero do �ltimo Item Vendido</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>13</td>
	 * <td>Cliche do Propriet�rio</td>
	 * <td>186 caracteres ASCII</td>
	 * </tr>
	 * <tr>
	 * <td>14</td>
	 * <td>N�mero do Caixa</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>15</td>
	 * <td>N�mero da Loja</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>16</td>
	 * <td>Moeda</td>
	 * <td>2 caracteres ASCII</td>
	 * </tr>
	 * <tr>
	 * <td>17</td>
	 * <td>FLAGS FISCAIS</td>
	 * <td>1 byte</td>
	 * </tr>
	 * <tr>
	 * <td>18</td>
	 * <td>Minutos Ligada</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>19</td>
	 * <td>Minutos Imprimindo</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>20</td>
	 * <td>FLAG de Interven��o T�cnica</td>
	 * <td>1 byte</td>
	 * </tr>
	 * <tr>
	 * <td>21</td>
	 * <td>FLAG de EPROM Conectada</td>
	 * <td>1 byte</td>
	 * </tr>
	 * <tr>
	 * <td>22</td>
	 * <td>Valor Pago no �ltimo Cupom</td>
	 * <td>7 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>23</td>
	 * <td>Data e Hora Atual(DDMMAAHHMMSS)</td>
	 * <td>6 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>24</td>
	 * <td>Contadores dos Totalizadores N�o Sujeitos ao ICMS</td>
	 * <td>9 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>25</td>
	 * <td>Descri��o dos Totalizadores Nao Sujeitos ao ICMS</td>
	 * <td>9 totalizadores com 19 caracteres</td>
	 * </tr>
	 * <tr>
	 * <td>26</td>
	 * <td>Data da �ltima Redu��o (DDMMAA)</td>
	 * <td>3 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>27</td>
	 * <td>Data do Movimento (DDMMAA)</td>
	 * <td>3 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>28</td>
	 * <td>FLAG de Truncamento</td>
	 * <td>1 byte</td>
	 * </tr>
	 * <tr>
	 * <td>29</td>
	 * <td>FLAG de Vincula��o ao ISS</td>
	 * <td>2 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>30</td>
	 * <td>Totalizador de Acr�scimo</td>
	 * <td>7 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>31</td>
	 * <td>Contador de Bilhete de Passagem</td>
	 * <td>3 bytes</td>
	 * </tr>
	 * <tr>
	 * <td>32</td>
	 * <td>Formas de Pagamento</td>
	 * <td></td>
	 * </tr>
	 * </table> <br>
	 * Para mais informa��es consulte a documenta��o da impressora.<br>
	 * <br>
	 * 
	 * @param var
	 *            indica��o da informa��o.<br>
	 * 
	 * @return informa��o da impressora.<br>
	 */
	public String retornoVariaveis( final char var ) {

		final byte[] CMD = { ESC, 35, (byte) var };
		/*
		 * o tamanho dos bytes de retorno varia conforme o parametro.
		 */
		executaCmd( CMD, 0 );

		String retorno = "";

		if ( var == V_NUM_SERIE 
				|| var == V_CNPJ_IE 
					|| var == V_CLICHE 
						|| var == V_MOEDA
							|| var == V_DEPARTAMENTOS ) {
			retorno = new String( getBytesLidos() );
		} else if ( var == V_DT_ULT_REDUCAO ) {
			retorno = bcdToAsc( getBytesLidos() ).substring( 0, 6 );
		} else {
			retorno = bcdToAsc( getBytesLidos() );
		}

		return retorno;
	}

	/**
	 * Este comando s� ter� efeito quando a impressora indicar �Pouco Papel.<br>
	 * A impressora retorna ACK n1 n2 ST1 ST2. Onde n1+(n2*256) � o n�mero de linhas impressas na condi��o de �Pouco Papel.<br>
	 * 
	 * @return estado do papel<br>
	 */
	public String retornoEstadoPapel() {

		final byte[] CMD = { ESC, 62, 54 };

		executaCmd( CMD, 5 );

		return bcdToAsc( getBytesLidos() );
	}

	/**
	 * Leitura dos Dados da �ltima Redu��o.<br>
	 * 
	 * @return �ltima redu��o<br>
	 */
	public String retornoUltimaReducao() {

		final byte[] CMD = { ESC, 62, 55 };

		executaCmd( CMD, 308 );

		return bcdToAsc( getBytesLidos() );
	}

	/**
	 * Com este comando pode-se programar a moeda no singular.<br>
	 * Este comando possui o par�metro com a descri��o da moeda no tamanho de 19 bytes.<br>
	 * 
	 * @param nomeSingular
	 *            descri��o.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaMoedaSingular( final String nomeSingular ) {

		byte[] CMD = { ESC, 58 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( nomeSingular, 19, false ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Com este comando pode-se programar a moeda no plural.<br>
	 * Este comando possui o par�metro com a descri��o da moeda no tamanho de 22 bytes.<br>
	 * 
	 * @param nomePlurar
	 *            descri��o.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int programaMoedaPlural( final String nomePlurar ) {

		byte[] CMD = { ESC, 59 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( nomePlurar, 19, false ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		return executaCmd( CMD, 3 );
	}

	/**
	 * Este comando retorna pela porta serial 1 byte correspondendo ao estado atual de<br>
	 * inser��o ou n�o do cheque.<br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public String retornoStatusCheque() {

		byte[] CMD = { ESC, 62, 48 };
		
		executaCmd( CMD, 3 );

		return bcdToAsc( getBytesLidos() );
	}

	/**
	 * <br>
	 * 
	 * @return estado da impressora.<br>
	 */
	public int cancelaImpressaoCheque() {

		byte[] CMD = { ESC, 62, 49 };
		
		return executaCmd( CMD, 3 );
	}

	/**
	 * Fun��o para impress�o de cheques.<br>
	 * <br>
	 * 
	 * @param valor
	 *            valor do cheque.
	 * @param favorecido
	 *            favorecido pelo cheque.
	 * @param localidade
	 *            pra�a do cheque
	 * @param dia
	 *            dia para composi��o da data.
	 * @param mes
	 *            m�s para composi��o da data.
	 * @param ano
	 *            ano para composi��o da data.
	 * 
	 * @return estado da impressora.<br>
	 */
	public int imprimeCheque( final float valor, final String favorecido, final String localidade, final int dia, final int mes, final int ano ) {

		byte[] CMD = { ESC, 57 };

		final StringBuffer buf = new StringBuffer();

		buf.append( parseParam( valor, 14, 2 ) );
		buf.append( parseParam( favorecido, 45, false ) );
		buf.append( parseParam( localidade, 27, false ) );
		buf.append( parseParam( dia, 2 ) );
		buf.append( parseParam( mes, 2 ) );
		buf.append( parseParam( ano, 4 ) );

		CMD = adicBytes( CMD, buf.toString().getBytes() );

		final byte[] posicoes = { 55, 10, 1, 6, 18, 50, 54, 71, 2, 5, 8, 10, 12, 0 };

		CMD = adicBytes( CMD, posicoes );

		return executaCmd( CMD, 3 );
	}

	public void aguardaImpressao() {

		byte[] CMD = { ESC, 19 };
		byte[] retorno = new byte[ 1 ];
		CMD = preparaCmd( CMD );

		while ( retorno.length < 2 ) {

			// depois que entra do la�o e ocorre algum erro no envio do comando
			// a condi��o de retorno == null valida o la�o
			// tornando ele um la�o infinito...

			retorno = enviaCmd( CMD, 3 );

			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e ) {
			}
		}
	}

	public int habilitaCupomAdicional( final char opt ) {

		byte[] CMD = { ESC, 68 };

		CMD = adicBytes( CMD, parseParam( opt, 1 ).getBytes() );

		return executaCmd( CMD, 3 );
	}

	public int resetErro() {

		final byte[] CMD = { ESC, 70 };

		return executaCmd( CMD, 3 );
	}
}
