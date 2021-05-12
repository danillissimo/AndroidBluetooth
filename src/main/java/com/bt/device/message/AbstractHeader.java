package com.bt.device.message;

import com.bt.device.BTCommunicator;
import com.bt.device.message.incoming.AbstractIncomingMessage;

import java.io.IOException;

import androidx.annotation.NonNull;

public interface AbstractHeader extends AbstractIncomingMessage, AbstractCommandHeader {

}
