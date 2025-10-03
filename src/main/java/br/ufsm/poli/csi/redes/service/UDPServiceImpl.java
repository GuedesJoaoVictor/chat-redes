package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Mensagem;
import br.ufsm.poli.csi.redes.model.TipoMensagem;
import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServiceImpl implements UDPService {

    private UDPServiceUsuarioListener usuarioListener = null;
    private UDPServiceMensagemListener mensagemListener = null;
    private Usuario usuario = null;

    public UDPServiceImpl() {
        new Thread(new EnviaSonda()).start();
        new Thread(new RecebeSonda()).start();
    }

    private class EnviaSonda implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(5000);
                if (usuario == null) {
                    continue;
                }
                Mensagem mensagem = new Mensagem();
                mensagem.setTipoMensagem(TipoMensagem.sonda);
                mensagem.setUsuario(usuario);
                mensagem.setStatus(usuario.getStatus().toString());
                ObjectMapper mapper = new ObjectMapper();
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes();
                DatagramPacket pacote = new DatagramPacket(
                        bMensagem,
                        bMensagem.length
                );
                DatagramSocket socket = new DatagramSocket();
                for (int i = 1; i < 255; i++) {
//                    pacote.setAddress(InetAddress.getByName("192.168.83." + i));
                    pacote.setAddress(InetAddress.getByName("255.255.255.255"));
                    pacote.setPort(8080);
                    socket.send(pacote);
                }
            }
        }

    }

    private class RecebeSonda implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket socket = new DatagramSocket(8080);
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                ObjectMapper mapper = new ObjectMapper();
                Mensagem message = null;
                try {
                    message = mapper.readValue(str, Mensagem.class);
                    usuarioAlterado(message.getUsuario());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(message);
            }
        }
    }

    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) {

    }

    @Override
    public void usuarioAlterado(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public void addListenerMensagem(UDPServiceMensagemListener listener) {
        this.mensagemListener = listener;
    }

    @Override
    public void addListenerUsuario(UDPServiceUsuarioListener listener) {
        this.usuarioListener = listener;
    }
}