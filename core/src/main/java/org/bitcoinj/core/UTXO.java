/*
 * Copyright 2012 Matt Corallo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.bitcoinj.script.*;
import com.google.common.base.Objects;

import java.io.*;
import java.math.*;
import java.util.Date;
import java.util.Locale;

// TODO: Fix this class: should not talk about addresses, height should be optional/support mempool height etc

/**
 * A UTXO message contains the information necessary to check a spending transaction.
 * It avoids having to store the entire parentTransaction just to get the hash and index.
 * Useful when working with free standing outputs.
 */
public class UTXO implements Serializable {

    private static final long serialVersionUID = 4736241649298988166L;

    private Coin value;
    private Script script;
    private Sha256Hash hash;
    private long index;
    private int height;
    private boolean coinbase;
    private String address;
    private String hashStr;
    private String preHash;
    private String txid;
    private Coin fee;
    private long timereceived;
    private String outputTxid;
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script,
                String address,String txid,Coin fee,long timereceived,String outputTxid) {
        this(hash, index, value, height, coinbase, script,address,txid,fee,timereceived);
        this.outputTxid = outputTxid;
    }
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script,
                String address,String txid,Coin fee,long timereceived) {
        this(hash, index, value, height, coinbase, script,address,txid,fee);
        this.timereceived = timereceived;
    }
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script,
                String address,String txid,Coin fee) {
        this(hash, index, value, height, coinbase, script,address,txid);
        this.fee = fee;
    }
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script,
                String address,String txid) {
        this(hash, index, value, height, coinbase, script);
        this.address = address;
        this.txid = txid;
    }

    /**
     * Creates a stored transaction output.
     *
     * @param hash     The hash of the containing transaction.
     * @param index    The outpoint.
     * @param value    The value available.
     * @param height   The height this output was created in.
     * @param coinbase The coinbase flag.
     */
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script) {
        this.hash = hash;
        this.index = index;
        this.value = value;
        this.height = height;
        this.script = script;
        this.coinbase = coinbase;
        this.address = "";
    }

    /**
     * Creates a stored transaction output.
     *
     * @param hash     The hash of the containing transaction.
     * @param index    The outpoint.
     * @param value    The value available.
     * @param height   The height this output was created in.
     * @param coinbase The coinbase flag.
     * @param address  The address.
     */
    public UTXO(Sha256Hash hash,
                long index,
                Coin value,
                int height,
                boolean coinbase,
                Script script,
                String address) {
        this(hash, index, value, height, coinbase, script);
        this.address = address;
    }

    public UTXO(InputStream in) throws IOException {
        deserializeFromStream(in);
    }

    /** The value which this Transaction output holds. */
    public Coin getValue() {
        return value;
    }

    /** The Script object which you can use to get address, script bytes or script type. */
    public Script getScript() {
        return script;
    }

    /** The hash of the transaction which holds this output. */
    public Sha256Hash getHash() {
        return hash;
    }

    /** The index of this output in the transaction which holds it. */
    public long getIndex() {
        return index;
    }

    /** Gets the height of the block that created this output. */
    public int getHeight() {
        return height;
    }

    /** Gets the flag of whether this was created by a coinbase tx. */
    public boolean isCoinbase() {
        return coinbase;
    }

    /** The address of this output, can be the empty string if none was provided at construction time or was deserialized */
    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Stored TxOut of %s (%s:%d)", value.toFriendlyString(), hash, index);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIndex(), getHash());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UTXO other = (UTXO) o;
        return getIndex() == other.getIndex() && getHash().equals(other.getHash());
    }

    public void serializeToStream(OutputStream bos) throws IOException {
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(value.value), bos);
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(fee.getValue()),bos);
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(timereceived),bos);
        byte[] scriptBytes = script.getProgram();
        Utils.uint32ToByteStreamLE(scriptBytes.length, bos);
        bos.write(scriptBytes);
        bos.write(hash.getBytes());
        Utils.uint32ToByteStreamLE(index, bos);
        Utils.uint32ToByteStreamLE(height, bos);
        bos.write(new byte[] { (byte)(coinbase ? 1 : 0) });
        byte[] txidBytes = txid.getBytes();
        Utils.uint32ToByteStreamBE(txidBytes.length,bos);
        bos.write(txidBytes);
    }

    public void deserializeFromStream(InputStream in) throws IOException {
        byte[] valueBytes = new byte[8];
        if (in.read(valueBytes, 0, 8) != 8)
            throw new EOFException();
        value = Coin.valueOf(Utils.readInt64(valueBytes, 0));

        byte[] feeBytes = new byte[8];
        if (in.read(feeBytes, 0, 8) != 8)
            throw new EOFException();
        fee = Coin.valueOf(Utils.readInt64(feeBytes, 8));

        byte[] timeBytes = new byte[8];
        if (in.read(feeBytes, 0, 8) != 8)
            throw new EOFException();
        timereceived = Utils.readInt64(timeBytes, 16);

        int scriptBytesLength = (int) Utils.readUint32FromStream(in);
        byte[] scriptBytes = new byte[scriptBytesLength];
        if (in.read(scriptBytes) != scriptBytesLength)
            throw new EOFException();
        script = new Script(scriptBytes);

        byte[] hashBytes = new byte[32];
        if (in.read(hashBytes) != 32)
            throw new EOFException();
        hash = Sha256Hash.wrap(hashBytes);

        byte[] indexBytes = new byte[4];
        if (in.read(indexBytes) != 4)
            throw new EOFException();
        index = Utils.readUint32(indexBytes, 0);

        height = (int) Utils.readUint32FromStream(in);

        byte[] coinbaseByte = new byte[1];
        in.read(coinbaseByte);
        coinbase = coinbaseByte[0] == 1;
        int txidLength = (int)Utils.readUint32FromStream(in);
        byte[] txidBytes = new byte[txidLength];
        in.read(txidBytes);
    }


    private void writeObject(ObjectOutputStream o) throws IOException {
        serializeToStream(o);
    }

    private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
        deserializeFromStream(o);
    }

    public String getHashStr() {
        return hashStr;
    }

    public void setHashStr(String hashStr) {
        this.hashStr = hashStr;
    }

    public String getPreHash() {
        return preHash;
    }

    public void setPreHash(String preHash) {
        this.preHash = preHash;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public Coin getFee() {
        return fee;
    }

    public void setFee(Coin fee) {
        this.fee = fee;
    }

    public long getTimereceived() {
        return timereceived;
    }

    public void setTimereceived(long timereceived) {
        this.timereceived = timereceived;
    }

    public String getOutputTxid() {
        return outputTxid;
    }

    public void setOutputTxid(String outputTxid) {
        this.outputTxid = outputTxid;
    }
}
