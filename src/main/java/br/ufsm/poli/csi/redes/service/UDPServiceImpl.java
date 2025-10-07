package br.ufsm.poli.csi.redes.service;

import br.ufsm.poli.csi.redes.model.Mensagem;
import br.ufsm.poli.csi.redes.model.TipoMensagem;
import br.ufsm.poli.csi.redes.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class UDPServiceImpl implements UDPService {

    private UDPServiceUsuarioListener usuarioListener = null;
    private UDPServiceMensagemListener mensagemListener = null;
    private Usuario usuario = null;
    private final Set<Usuario> usuariosSet = new HashSet<>();
    private final Set<Usuario> activeUsers = new HashSet<>();

    public UDPServiceImpl() {
        new Thread(new EnviaSonda()).start();
        // new Thread(new TesteMensagem()).start();
        new Thread(new RecebeSonda()).start();
        new Thread(new ActiveUsers()).start();
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
                pacote.setPort(8080);
                for (int i = 1; i < 255; i++) {
                    String endereco = "192.168.83." + i;
                    if (!endereco.equals(InetAddress.getLocalHost().getHostAddress())) {
                        pacote.setAddress(InetAddress.getByName(endereco));
                        //pacote.setAddress(InetAddress.getByName("255.255.255.255"));
                        socket.send(pacote);
                    }
                }
                socket.close();
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
                    switch (message.getTipoMensagem()) {
                        case sonda -> {
                            if (usuariosSet.add(message.getUsuario())) {
                                usuarioListener.usuarioAdicionado(message.getUsuario());
                            } else {
                                usuarioListener.usuarioAlterado(message.getUsuario());
                            }
                            activeUsers.add(message.getUsuario());
                        }
                        case msg_individual -> {
                            mensagemListener.mensagemRecebida(message.getMsg(), message.getUsuario(), false);
                        }
                        case msg_grupo -> {
                            String enderecoUsuario = message.getUsuario().getEndereco().getLocalHost().getHostAddress();
                            if (!enderecoUsuario.equals(InetAddress.getLocalHost().getHostAddress())) {
                                mensagemListener.mensagemRecebida(message.getMsg(), message.getUsuario(), true);
                            }
                        }
                        case fim_chat -> {
                            usuariosSet.remove(message.getUsuario());
                            usuarioListener.usuarioRemovido(message.getUsuario());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(message);
            }
        }
    }

    private class ActiveUsers implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while(true) {
                Thread.sleep(30000);
                usuariosSet.forEach(usuario -> {
                    if (!activeUsers.contains(usuario)) {
                        usuariosSet.remove(usuario);
                    }
                });
                activeUsers.clear();
            }
        }
    }

    @SneakyThrows
    @Override
    public void enviarMensagem(String mensagem, Usuario destinatario, boolean chatGeral) {
        Mensagem msg = Mensagem.builder()
                .tipoMensagem(chatGeral ? TipoMensagem.msg_grupo : TipoMensagem.msg_individual)
                .usuario(this.usuario)
                .msg(mensagem)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String messageString = mapper.writeValueAsString(msg);
        byte[] bMensagem = messageString.getBytes();
        DatagramPacket pacote = new DatagramPacket(bMensagem, bMensagem.length);
        DatagramSocket socket = new DatagramSocket();
        if (chatGeral && destinatario.getNome().equals("Geral")) {
            socket.setBroadcast(true);
            pacote.setAddress(InetAddress.getByName("192.168.95.255"));
        } else {
            pacote.setAddress(destinatario.getEndereco());
        }
        pacote.setPort(8080);
        socket.send(pacote);
        socket.close();
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

    private class TesteMensagem implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while(true) {
                Thread.sleep(5000);
                Mensagem mensagem = Mensagem.builder()
                        .tipoMensagem(TipoMensagem.msg_individual)
                        .status(usuario.getStatus().toString())
                        .usuario(usuario)
                        .msg("Hello World!")
                        .build();
                ObjectMapper mapper = new ObjectMapper();
                String strMensagem = mapper.writeValueAsString(mensagem);
                byte[] bMensagem = strMensagem.getBytes();
                DatagramPacket packet = new DatagramPacket(bMensagem, bMensagem.length);
                DatagramSocket socket = new DatagramSocket();
//                for (int i = 1; i < 255; i++) {
//                    packet.setAddress(InetAddress.getByName("192.168.83." + i));
                    packet.setAddress(InetAddress.getByName("255.255.255.255"));
                    packet.setPort(8080);
                    socket.send(packet);
                    socket.close();
//                }
            }
        }
    }

}