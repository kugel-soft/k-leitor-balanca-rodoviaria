package com.github.kugelsoft.leitorbalancarodoviaria;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;

public abstract class ComunicacaoBalanca {

	protected ParametrosBalanca parametros;

	protected ComunicacaoBalanca(ParametrosBalanca parametros) {
		this.parametros = parametros;
	}

	protected String substring(String retorno, int indexIniInclusive, int indexFimExclusive) {
		if (retorno == null || indexIniInclusive >= retorno.length()) {
			return "";
		}
		if (indexFimExclusive > retorno.length()) {
			indexFimExclusive = retorno.length();
		}
		return retorno.substring(indexIniInclusive, indexFimExclusive);
	}

	protected String enviarComando(String cmd) throws IOException {
		return enviarComando(cmd, 0);
	}

	protected String enviarComando(String cmd, int minBytes) throws IOException {
		String retorno = "";
		Socket socket = null;
		try {
			socket = new Socket(parametros.getIp(), parametros.getPorta());

			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();

			if (cmd != null) {
				outputStream.write(cmd.getBytes());
				outputStream.flush();
				System.out.println("Enviou: " + cmd);

				int tentativas = 0;
				do {
					byte[] bytes = new byte[256];
					inputStream.read(bytes);
					retorno = rightTrim(new String(bytes));
					System.out.println("Recebeu: " + retorno);
					tentativas++;
				} while (retorno.length() < minBytes && tentativas < 10);
			}

			socket.close();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return retorno;
	}

	protected String rightTrim(String str) {
		int len = str.length();
		while (len > 0 && str.charAt(len - 1) <= ' ') {
			len--;
		}
		return ((len < str.length())) ? str.substring(0, len) : str;
	}

	protected abstract BigDecimal lerPesoModelo() throws IOException, PesoInvalidoException, PesoInstavelException;

	/**
	 * Faz a leitura do peso da balan??a
	 * @return BigDecimal com o peso
	 * @throws IOException Se ocorrer algum erro de comunica????o com a balan??a
	 * @throws PesoInvalidoException Caso o peso esteja inv??lido
	 * @throws PesoInstavelException Caso o peso da balan??a n??o esteja est??vel
	 */
	public BigDecimal lerPeso() throws IOException, PesoInvalidoException, PesoInstavelException {
		BigDecimal peso = lerPesoModelo();

		int cont = parametros.getQuantidadeLeiturasConsiderarPesoEstavel();
		while (cont > 1) {
			try {
				Thread.sleep(parametros.getMilissegundosEntreLeiturasConsiderarPesoEstavel());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			BigDecimal novoPeso = lerPesoModelo();
			BigDecimal difQuantPeso = peso.subtract(novoPeso).abs();
			if (difQuantPeso.doubleValue() > parametros.getPesoToleranciaConsiderarPesoEstavel()) {
				throw new PesoInstavelException();
			}
			cont--;
		}
		return peso;
	}

	/**
	 * Testa a conex??o com a balan??a e gera uma IOException se ocorrer algum erro de comunica????o
	 * @throws IOException se ocorrer algum erro de comunica????o
	 */
	public abstract void testarConexao() throws IOException;
}
