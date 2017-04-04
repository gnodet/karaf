package org.jline.reader.impl;

import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;
import org.aesh.util.LoggerUtil;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.Curses;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerminalConnection implements Connection, Device {

    private static final Logger LOGGER = LoggerUtil.getLogger(TerminalConnection.class.getName());

    private final Terminal terminal;
    private Attributes attributes;
    private volatile boolean reading = false;
    private Consumer<Size> sizeHandler;
    private Consumer<Void> closeHandler;
    private Consumer<int[]> stdinHandler;
    private Consumer<int[]> stdoutHandler;
    private Consumer<Signal> signalHandler;
    private Charset charset = Charset.defaultCharset();
    private Terminal.SignalHandler prevIntHandler;
    private Terminal.SignalHandler prevWinchHandler;

    public TerminalConnection(Terminal terminal) {
        this.terminal = terminal;
        this.stdoutHandler = data -> write(new String(data, 0, data.length));
    }

    @Override
    public Consumer<Signal> getSignalHandler() {
        return signalHandler;
    }

    @Override
    public void setSignalHandler(Consumer<Signal> signalHandler) {
        this.signalHandler = signalHandler;
    }

    @Override
    public Consumer<Size> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Size> sizeHandler) {
        this.sizeHandler = sizeHandler;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return closeHandler;
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return stdinHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> stdinHandler) {
        this.stdinHandler = stdinHandler;
    }

    public Consumer<int[]> stdoutHandler() {
        return stdoutHandler;
    }

    @Override
    public Size size() {
        return toAeshSize(terminal.getSize());
    }

    private Size toAeshSize(org.jline.terminal.Size size) {
        return new Size(size.getColumns(), size.getRows());
    }

    @Override
    public Charset inputEncoding() {
        return charset;
    }

    @Override
    public Charset outputEncoding() {
        return charset;
    }

    public void openBlocking(String buffer) {
        try {
            reading = true;
            if (buffer != null) {
                stdinHandler.accept(buffer.codePoints().toArray());
            }
            attributes = terminal.enterRawMode();
//                Attributes att = terminal.getAttributes();
//                att.setLocalFlag(Attributes.LocalFlag.ISIG, false);
//                terminal.setAttributes(att);
            //interrupt signal
            prevIntHandler = this.terminal.handle(org.jline.terminal.Terminal.Signal.INT, s -> {
                if(getSignalHandler() != null) {
                    getSignalHandler().accept(convert(org.aesh.terminal.tty.Signal.class, s));
                }
                else {
                    LOGGER.log(Level.FINE, "No signal handler is registered, lets stop");
                    close();
                }
            });
            //window resize signal
            prevWinchHandler = this.terminal.handle(org.jline.terminal.Terminal.Signal.WINCH, s -> {
                if(getSizeHandler() != null) {
                    getSizeHandler().accept(size());
                }
            });
            while (reading) {
                int read = terminal.reader().read(10);
                if (read == NonBlockingReader.READ_EXPIRED) {
                    continue;
                }
                if (read > 0) {
                    stdinHandler.accept(new int[]{read});
                } else {
                    if (getCloseHandler() != null)
                        getCloseHandler().accept(null);
                    close();
                    return;
                }
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Failed while reading, exiting", ioe);
            if (getCloseHandler() != null)
                getCloseHandler().accept(null);
            close();
        }
    }

    @Override
    public void openBlocking() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void openNonBlocking() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        reading = false;
        if (terminal != null) {
            if (attributes != null) {
                terminal.setAttributes(attributes);
            }
            if (prevIntHandler != null) {
                terminal.handle(org.jline.terminal.Terminal.Signal.INT, prevIntHandler);
            }
            if (prevWinchHandler != null) {
                terminal.handle(org.jline.terminal.Terminal.Signal.WINCH, prevWinchHandler);
            }
            if (closeHandler != null) {
                closeHandler.accept(null);
            }
        }
    }

    @Override
    public Connection write(String s) {
        terminal.writer().write(s);
        terminal.flush();
        return this;
    }

    @Override
    public org.aesh.terminal.Attributes getAttributes() {
        return toAeshAttributes(terminal.getAttributes());
    }

    @Override
    public void setAttributes(org.aesh.terminal.Attributes attributes) {
        terminal.setAttributes(toJlineAttributes(attributes));
    }

    @Override
    public org.aesh.terminal.Attributes enterRawMode() {
        return toAeshAttributes(terminal.enterRawMode());
    }

    private Attributes toJlineAttributes(org.aesh.terminal.Attributes attributes) {
        Attributes attr = new Attributes();
        convert(org.aesh.terminal.Attributes.ControlFlag.class, Attributes.ControlFlag.class, attributes::getControlFlag, attr::setControlFlag);
        convert(org.aesh.terminal.Attributes.InputFlag.class, Attributes.InputFlag.class, attributes::getInputFlag, attr::setInputFlag);
        convert(org.aesh.terminal.Attributes.LocalFlag.class, Attributes.LocalFlag.class, attributes::getLocalFlag, attr::setLocalFlag);
        convert(org.aesh.terminal.Attributes.OutputFlag.class, Attributes.OutputFlag.class, attributes::getOutputFlag, attr::setOutputFlag);
        convert(org.aesh.terminal.Attributes.ControlChar.class, Attributes.ControlChar.class, attributes::getControlChar, attr::setControlChar);
        return attr;
    }

    private org.aesh.terminal.Attributes toAeshAttributes(Attributes attributes) {
        org.aesh.terminal.Attributes attr = new org.aesh.terminal.Attributes();
        convert(Attributes.ControlFlag.class, org.aesh.terminal.Attributes.ControlFlag.class, attributes::getControlFlag, attr::setControlFlag);
        convert(Attributes.InputFlag.class, org.aesh.terminal.Attributes.InputFlag.class, attributes::getInputFlag, attr::setInputFlag);
        convert(Attributes.LocalFlag.class, org.aesh.terminal.Attributes.LocalFlag.class, attributes::getLocalFlag, attr::setLocalFlag);
        convert(Attributes.OutputFlag.class, org.aesh.terminal.Attributes.OutputFlag.class, attributes::getOutputFlag, attr::setOutputFlag);
        convert(Attributes.ControlChar.class, org.aesh.terminal.Attributes.ControlChar.class, attributes::getControlChar, attr::setControlChar);
        return attr;
    }

    @Override
    public Device device() {
        return this;
    }

    @Override
    public String type() {
        return terminal.getType();
    }

    @Override
    public boolean getBooleanCapability(Capability capability) {
        return terminal.getBooleanCapability(convert(org.jline.utils.InfoCmp.Capability.class, capability));
    }

    @Override
    public Integer getNumericCapability(Capability capability) {
        return terminal.getNumericCapability(convert(org.jline.utils.InfoCmp.Capability.class, capability));
    }

    @Override
    public String getStringCapability(Capability capability) {
        return terminal.getStringCapability(convert(org.jline.utils.InfoCmp.Capability.class, capability));
    }

    @Override
    public int[] getStringCapabilityAsInts(Capability capability) {
        try {
            String ks = getStringCapability(capability);
            if (ks != null) {
                StringWriter sw = new StringWriter();
                Curses.tputs(sw, ks);
                return sw.toString().codePoints().toArray();
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    @Override
    public boolean puts(Consumer<int[]> consumer, Capability capability) {
        int[] ks = getStringCapabilityAsInts(capability);
        if (ks != null) {
            consumer.accept(ks);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean put(Capability capability, Object... objects) {
        return terminal.puts(convert(org.jline.utils.InfoCmp.Capability.class, capability), objects);
    }

    private <F extends Enum<F>, T extends Enum<T>, V> void convert(Class<F> from, Class<T> to, Function<F, V> getter, BiConsumer<T, V> setter) {
        for (F f : from.getEnumConstants()) {
            setter.accept(convert(to, f), getter.apply(f));
        }
    }

    private <U extends Enum<U>, V extends Enum<V>> U convert(Class<U> ucls, V v) {
        return Enum.valueOf(ucls, v.name());
    }

}
