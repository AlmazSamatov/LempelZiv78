import javafx.util.Pair;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;


/**
 * Created by Almaz on 31.10.2017.
 */
public class LZ78 {

    private class Node {
        Node[] next = new Node[256];
        int indexInDict;

        Node(int indexInDict){
            this.indexInDict = indexInDict;
        }

        Node(){
            indexInDict = 0;
        }
    }

    private byte[] getBytes(File file) throws IOException {
        return Files.readAllBytes(Paths.get(file.getPath()));
    }

    private BitSet getBits(File file) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(file.getPath()));
        BitSet bitSet = new BitSet(8 * bytes.length);
        for (int j = 0; j < bytes.length; j++) {
            String binary = byteToBinary(bytes[j]);
            int y = 0;
            // parse byte in reverser order, because our function toByteArray() of BitSet transform it in little-endian
            for (int i = 7; i >= 0; i--) {
                if(binary.charAt(i) == '1')
                    bitSet.set(j * 8 + y);
                y++;
            }
        }
        return bitSet;
    }

    private String byteToBinary(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    private String intToBinary(int i){
        return String.format("%32s", Integer.toBinaryString(i)).replace(' ', '0');
    }

    private String shortToBinary(short s){
        return String.format("%16s", Integer.toBinaryString(s & 0xFFFF)).replace(' ', '0');
    }

    private short binaryToShort(String s){
        return Short.parseShort(s, 2);
    }

    private int binaryToInt(String s){
        return Integer.parseInt(s, 2);
    }

    private byte binaryToByte(String s){
        return (byte) ((byte) Integer.parseInt(s, 2) & 0xFF);
    }

    private class Trie {
        Node root = new Node();
        int entriesInDict = 0;
        boolean isTwoBytesRequiredForNum = true; // whether 2 bytes enough for coding number in dictionary
        ArrayList<Pair<Integer, Byte>> arrayList;

        void buildTree(byte[] word){
            Node curNode = root;
            arrayList = new ArrayList<>();

            for (int i = 0; i < word.length; i++) {
                if (curNode.next[word[i] + 128] == null) {
                    // if we did not find any continue of tree that we need
                    // create new leaf node
                    curNode.next[word[i] + 128] = new Node();
                    arrayList.add(new Pair<>(curNode.indexInDict, word[i]));
                    entriesInDict++;
                    curNode.next[word[i] + 128].indexInDict = entriesInDict;
                    // and begin again with root node
                    curNode = root;
                    continue;
                } else if(i == word.length - 1){
                    // if we in the end of file and there is no other symbol and we just write number of entry in dictionary
                    // with 0 byte
                    curNode = curNode.next[word[i] + 128];
                    arrayList.add(new Pair<>(curNode.indexInDict, (byte)0));
                }
                curNode = curNode.next[word[i] + 128];
            }

            // if not then use integer (4 bytes)
            if(entriesInDict > Short.MAX_VALUE)
                isTwoBytesRequiredForNum = false;
        }
    }

    private String bitSetToString(BitSet b, int size){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < size; i++){
            if(b.get(i))
                stringBuilder.append('1');
            else
                stringBuilder.append('0');
        }
        return stringBuilder.toString();
    }

    private BitSet compressedFileToBinary(ArrayList<Pair<Integer, Byte>> arr,
                                          boolean isTwoBytesRequired) {
        BitSet bitSet;
        if (isTwoBytesRequired){
            bitSet = new BitSet(arr.size() * 24 + 1);
        } else {
            bitSet = new BitSet(arr.size() * 40 + 1);
            bitSet.set(0);
        }
        int i = 1;
        for (Pair<Integer, Byte> anArr : arr) {
            if (isTwoBytesRequired) {
                // translate short to binary
                String s = shortToBinary(anArr.getKey().shortValue());
                for(int j = 0; j < 16; j++)
                    if(s.charAt(j) == '1')
                        bitSet.set(i + j);
                i += 16;
            } else {
                // translate int to binary
                String s = intToBinary(anArr.getKey());
                for(int j = 0; j < 32; j++)
                    if(s.charAt(j) == '1')
                        bitSet.set(i + j);
                i += 32;
            }
            // translate byte to binary
            String s = byteToBinary(anArr.getValue());
            for(int j = 0; j < 8; j++)
                if(s.charAt(j) == '1')
                    bitSet.set(i + j);
            i+=8;
        }
        return bitSet;
    }


    void compress(File file) throws IOException {
        Trie myTrie = new Trie();
        byte[] bytes = getBytes(file);
        // build a tree
        myTrie.buildTree(bytes);
        // get compressed file in bits
        BitSet compressedFileInBits = compressedFileToBinary(myTrie.arrayList, myTrie.isTwoBytesRequiredForNum);
        // translate it to byte array
        byte data[] = compressedFileInBits.toByteArray();
        String name = file.getName();
        Path p = Paths.get("./" + "compressed_" + name);

        // create new file with name compressed_+old_name_of_file
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(p))) {
            out.write(data, 0, data.length);
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    void decompress(File file) throws IOException {

        BitSet compressedFile = getBits(file);
        boolean isTwoBytes = !compressedFile.get(0);
        ArrayList<Byte> bytes = new ArrayList<>(); // final representation of file

        ArrayList<ArrayList<Byte>> arrayList = new ArrayList<>(); // for storing dictionary
        if (isTwoBytes){
            // if work with shorts
            for(int i = 1; i < compressedFile.length(); i+=24){
                short temp = binaryToShort(bitSetToString(compressedFile.get(i, i + 16), 16));
                byte b = binaryToByte(bitSetToString(compressedFile.get(i + 16, i + 24), 8));
                ArrayList<Byte> a = new ArrayList<>();
                if(temp != 0)
                    a.addAll(arrayList.get(temp - 1));
                a.add(b);
                arrayList.add(a);
                bytes.addAll(a);
            }
        } else{
            // if work with integers
            for(int i = 1; i < compressedFile.length(); i+=40){
                int temp = binaryToInt(bitSetToString(compressedFile.get(i, i + 32), 32));
                byte b = binaryToByte(bitSetToString(compressedFile.get(i + 32, i + 40), 8));
                ArrayList<Byte> a = new ArrayList<>();
                if (temp != 0)
                    a.addAll(arrayList.get(temp - 1));
                a.add(b);
                arrayList.add(a);
                bytes.addAll(a);
            }
        }

        byte data[] = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); i++)
            data[i] = bytes.get(i);

        String name = file.getName();
        Path p = Paths.get("./" + "decompressed_" + name.substring(11));

        // create new file with name decompressed_+old_name_of_file
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(p))) {
            out.write(data, 0, data.length);
        } catch (IOException x) {
            System.err.println(x);
        }
    }


}
