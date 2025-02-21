package LinearRegression;

import com.n1analytics.paillier.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static LinearRegression.Constants.MSG_DELIM;
import static LinearRegression.Constants.NUM_DELIM;

public class CryptoWorker {
    private boolean hide_vals;
    private PaillierContext paillier_context;
    private PaillierPublicKey pub_key;
    private EncryptedNumber zero;
    private EncryptedNumber one;
    private EncryptedNumber normalizer_enc;
    private Obfuscator phi;
    private Obfuscator lambda;
    private Obfuscator phi_lambda;
    private Obfuscator eta;

    private ArrayList<String> host_list;
    private int port;
    private double normalizer;

    public CryptoWorker(PaillierPublicKey pub_key,
                        double alpha, double number_of_inputs,
                        String hosts, int port, boolean hide_vals) {
        this.hide_vals = hide_vals;
        this.host_list = make_host_list(hosts);
        this.port = port;
        this.pub_key = pub_key;
        this.paillier_context = pub_key.createSignedContext();
        this.zero = paillier_context.encrypt(0.0);
        this.one = paillier_context.encrypt(1.0);
        this.normalizer = alpha / number_of_inputs;
        this.normalizer_enc = paillier_context.encrypt(normalizer);
    }

    public CryptoWorker(PaillierPublicKey pub_key,
                        String hosts, int port, boolean hide_vals) {
        this.hide_vals = hide_vals;
        this.host_list = make_host_list(hosts);
        this.port = port;
        this.pub_key = pub_key;
        this.paillier_context = pub_key.createSignedContext();
        this.zero = paillier_context.encrypt(0.0);
        this.one = paillier_context.encrypt(1.0);
        this.normalizer = 0.0;
        this.normalizer_enc = paillier_context.encrypt(normalizer);
    }

    private static int check_neg(int n) {
        return n < 0 ? n * -1 : n;
    }



    private ArrayList<String> make_host_list(String hosts_raw) {
        ArrayList<String> ret_val = new ArrayList<>();
        Pattern ip_regex = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        for(String s : hosts_raw.split(",")) {
            Matcher matcher = ip_regex.matcher(s);
            if(matcher.matches()) {
                ret_val.add(s);
            }
        }
        return ret_val;
    }

    private int get_host_index() {
        SecureRandom rand = new SecureRandom();
        byte[] rand_bytes = new byte[4];
        rand.nextBytes(rand_bytes);
        int index = check_neg(ByteBuffer.wrap(rand_bytes).getChar());
        return index % this.host_list.size();
    }

    private String get_host() {
        return this.host_list.get(get_host_index());
    }

    private void generate_phi_lambda(ObfuscatorOperations op) {
        phi = new Obfuscator(paillier_context, op);
        lambda = new Obfuscator(paillier_context, op);
        try {
            phi_lambda = new Obfuscator(phi, lambda, paillier_context, op);
        } catch (ArithmeticException e) {
            // have to try it all again!
            System.err.println("ERROR: " + e.getMessage());
            generate_phi_lambda(op);
        }
    }

    private void generate_eta(ObfuscatorOperations op) {
        eta = new Obfuscator(paillier_context, op);
    }

    private String send_rcv_msg(String msg, boolean rcv_msg) {
        Socket s;
        try {
            s = new Socket(get_host(), port);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            if(rcv_msg) {
                byte[] ptext = msg.getBytes(StandardCharsets.UTF_8);
                out.write(ptext);
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                char[] raw_input = new char[1024];
                int rc = br.read(raw_input);
                if (rc < 0) {
                    System.err.println(String.format("ERROR: read %d from read rc", rc));
                }
                return new String(raw_input).trim();
            }
            s.close();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String two_arg_msg(MessageType mtype, Operations op,  EncryptedNumber a, EncryptedNumber b, String additional_msg) {
        String op_str = Utilities.op_to_str(op);
        String mtype_str = Utilities.msg_type_to_str(mtype);
        BigInteger ciphertext_a = a.calculateCiphertext();
        BigInteger ciphertext_b = b.calculateCiphertext();
        StringBuilder msg = new StringBuilder();
        String[] components;
        if (Objects.equals(additional_msg, "")) {
            components = new String[]{mtype_str, MSG_DELIM, op_str, MSG_DELIM,
                    ciphertext_a.toString(), NUM_DELIM, Integer.toString(a.getExponent()), MSG_DELIM,
                    ciphertext_b.toString(), NUM_DELIM, Integer.toString(b.getExponent())};
        } else {
            components = new String[]{mtype_str, MSG_DELIM, op_str, MSG_DELIM,
                    ciphertext_a.toString(), NUM_DELIM, Integer.toString(a.getExponent()), MSG_DELIM,
                    ciphertext_b.toString(), NUM_DELIM, Integer.toString(b.getExponent()),
                    MSG_DELIM, additional_msg};
        }
        for (String str : components) {
            msg.append(str);
        }
        return msg.toString();
    }

    private String two_arg_msg(MessageType mtype, Operations op,  double a, double b, String additional_msg) {
        String op_str = Utilities.op_to_str(op);
        String mtype_str = Utilities.msg_type_to_str(mtype);
        StringBuilder msg = new StringBuilder();
        String[] components;
        if (Objects.equals(additional_msg, "")) {
            components = new String[]{mtype_str,
                    MSG_DELIM, op_str,
                    MSG_DELIM, Double.toString(a),
                    MSG_DELIM, Double.toString(b)};
        } else {
            components = new String[]{mtype_str,
                    MSG_DELIM, op_str,
                    MSG_DELIM, Double.toString(a),
                    MSG_DELIM, Double.toString(b),
                    MSG_DELIM, additional_msg};
        }
        for (String str : components) {
            msg.append(str);
        }
        return msg.toString();
    }

    private EncryptedNumber send_op_enc(EncryptedNumber a, EncryptedNumber b, String additional_msg, Operations op) {
        String op_str = Utilities.op_to_str(op);
        switch (op) {
            case MULTIPLY:
                return remote_add_enc(a, b, additional_msg);
            case DIVIDE:
                return remote_divide_enc(a, b, additional_msg);
            case COMPARE:
                // TODO: fix this nonesense
                System.out.println("Don't Call this this way.");
                System.exit(0);
        }
        return null;
    }

    // TODO: this should be private and encourperated in above function
    public boolean remote_compare_enc(EncryptedNumber a, EncryptedNumber b, ComparisonOperator comp_op, String additional_msg) {
        if(hide_vals) {
            generate_eta(ObfuscatorOperations.COMPARE);
            System.out.println(eta.getDecimal().doubleValue());
            a = a.multiply(eta.getEncoded());
            b = b.multiply(eta.getEncoded());
        }
        String msg = two_arg_msg(MessageType.OPE, Operations.COMPARE, a, b, additional_msg);
        msg = String.format("%s|%s", msg, Utilities.comp_op_to_str(comp_op));
        String input_str = send_rcv_msg(msg.toString(), true);
        assert !(Objects.equals(input_str, null));
        return Integer.parseInt(input_str) == 1;
    }

    private EncryptedNumber remote_divide_enc(EncryptedNumber a, EncryptedNumber b, String additional_msg){
        if(hide_vals) {
            generate_phi_lambda(ObfuscatorOperations.DIVIDE);
            a = a.multiply(phi.getEncoded());
            b = b.multiply(lambda.getEncoded());
        }
        String msg = two_arg_msg(MessageType.OPE, Operations.DIVIDE, a, b, additional_msg);
        String input_str = send_rcv_msg(msg.toString(), true);
        assert !(Objects.equals(input_str, null));
        EncryptedNumber aug_ans = cast_encrypted_number_raw_split(input_str);
        if(hide_vals) {
            aug_ans = aug_ans.multiply(phi_lambda.getEncoded());
            remote_round(aug_ans);
        }
        return aug_ans;
    }

    private EncryptedNumber remote_add_enc(EncryptedNumber a, EncryptedNumber b, String additional_msg){
        if(hide_vals) {
            generate_phi_lambda(ObfuscatorOperations.MULTIPLY);
            a = add_enc(a, phi.getEncrypted());
            b = add_enc(b, lambda.getEncrypted());
        }
        String msg = two_arg_msg(MessageType.OPE, Operations.MULTIPLY, a, b, additional_msg);
        String input_str = send_rcv_msg(msg.toString(), true);
        assert !(Objects.equals(input_str, null));
        EncryptedNumber aug_ans = cast_encrypted_number_raw_split(input_str);
        if(hide_vals) {
            a = subtract_enc(a, phi.getEncrypted());
            b = subtract_enc(b, lambda.getEncrypted());
            EncryptedNumber first = a.multiply(lambda.getEncoded());
            first = remote_round(first);
            EncryptedNumber second = b.multiply(phi.getEncoded());
            second = remote_round(second);
            assert first != null && second != null;
            EncryptedNumber sub = add_enc(first, second);
            sub = add_enc(sub, phi_lambda.getEncrypted());
            aug_ans = subtract_enc(aug_ans, sub);
        }
        return aug_ans;
    }

    public EncryptedNumber remote_round(EncryptedNumber a) {
        String mtype_str = Utilities.msg_type_to_str(MessageType.ROUND);
        String msg = String.format("%s|%s#%d", mtype_str, a.calculateCiphertext().toString(), a.getExponent());
        String input_str = send_rcv_msg(msg, true);
        assert !(Objects.equals(input_str, null));
        EncryptedNumber aug_ans = cast_encrypted_number_raw_split(input_str);
        return aug_ans;
    }

    private double send_op_pt(double a, double b, String additional_msg, Operations op) {
        String op_str = Utilities.op_to_str(op);
        String msg = two_arg_msg(MessageType.OPD, op, a, b, additional_msg);
        String input_str = send_rcv_msg(msg.toString(), true);
        assert !(Objects.equals(input_str, null));
        return Double.parseDouble(input_str);
    }

    public EncryptedNumber remote_op(EncryptedNumber a, EncryptedNumber b, Operations op) {
        return this.send_op_enc(a, b, "", op);
    }

    public EncryptedNumber remote_op(EncryptedNumber a, EncryptedNumber b, String additional_msg, Operations op) {
        return this.send_op_enc(a, b, additional_msg, op);
    }

    public double remote_op(double a, double b, String additional_msg, Operations op) {
        return this.send_op_pt(a, b, additional_msg, op);
    }

    public double remote_op(double a, double b, Operations op) {
        return this.send_op_pt(a, b, "", op);
    }

    public void send_final_value(String name, EncryptedNumber a){
        send_value(name, a);
    }

    public void send_value(String name, EncryptedNumber a) {
        String mtype_str = Utilities.msg_type_to_str(MessageType.VALUE);
        String msg = String.format("%s|%s|%s#%d", mtype_str, name, a.calculateCiphertext().toString(), a.getExponent());
        send_rcv_msg(msg, false);
    }

    public void send_remote_msg(String base_msg) {
        String mtype_str = Utilities.msg_type_to_str(MessageType.MSG);
        String msg = String.format("%s|%s", mtype_str,  base_msg);
        send_rcv_msg(msg, false);
    }


    public EncryptedNumber add_enc(EncryptedNumber a, EncryptedNumber b) {
        assert a != null;
        assert b != null;
        try {
            a.checkSameContext(b);
        } catch (PaillierContextMismatchException e) {
            e.printStackTrace();
        }
        return a.add(b);
    }

    public EncryptedNumber subtract_enc(EncryptedNumber a, EncryptedNumber b) {
        try {
            a.checkSameContext(b);
        } catch (PaillierContextMismatchException e) {
            e.printStackTrace();
        }
        assert a != null;
        assert b != null;
        return a.subtract(b);
    }

    public EncryptedNumber get_zero() {
        return zero;
    }
    public EncryptedNumber get_one() {
        return one;
    }
    public EncryptedNumber get_normalizer_enc() {
        return normalizer_enc;
    }
    public double get_normalizer() {
        return normalizer;
    }
    public EncryptedNumber create_encrypted_number(BigInteger s) {
        return this.paillier_context.encrypt(s);
    }
    public EncryptedNumber create_encrypted_number(String s) {
        return paillier_context.encrypt(new BigInteger(s));
    }
    public EncryptedNumber create_encrypted_number(Double d) {
        return paillier_context.encrypt(d);
    }
    private EncryptedNumber cast_encrypted_number(String cipher_text, int exp) {
        return new EncryptedNumber(paillier_context, new BigInteger(cipher_text), exp);
    }
    public EncryptedNumber cast_encrypted_number(BigInteger cipher_text, int exp) {
        return new EncryptedNumber(paillier_context, cipher_text, exp);
    }
    public EncryptedNumber cast_encrypted_number_raw_split(String line) {
        String[] raw_split = line.split(NUM_DELIM);
        return cast_encrypted_number(raw_split[0], Integer.parseInt(raw_split[1]));
    }

    public EncodedNumber encode_number(BigInteger s) {
        return paillier_context.encode(s);
    }

}
