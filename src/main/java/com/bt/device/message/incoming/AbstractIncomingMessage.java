package com.bt.device.message.incoming;

import com.bt.BTInputStream;

import java.io.IOException;
import java.text.ParseException;

import androidx.annotation.NonNull;

public interface AbstractIncomingMessage {

    //  11.02.2021 Метод разбора строки - зло, т.к. поощряет предварительное прочтение всего
    //  объема данных перед разбором, что ведет к неоправданному расходу памяти. Его бы надо вырезать
    //  Метод разбора потока возвращает строку - зачем?
    //  И кидает StreamParseException
    //  В нем есть смысл когда разбираемые данные считываются целиком, да, в этом случае мы спасаем
    //  считанные данные от полной потери.
    //  Но мы уже выяснили что это плохой подход. А при правильном подходе спасать практически ничего
    //  не надо, только последнее слово на котором сбойнули, типа того. Все остальные данные либо уже
    //  прочтены и разобраны, либо еще даже не прочитаны. То есть для доступа к данным в случае ошибки
    //  надо выкидывать заполняемый объект и сбойнувшее слово

    //If one somehow wishes to parse a string, one should wrap it in an InputStream and in a
    //BTInputStream
    //void parse(@NonNull String data) throws ParseException;

    /**
     * In case of failure, data, read by this constructor, should be available trough
     * {@link StreamParseException#getData()}
     * @param input
     * @throws StreamParseException
     * @throws IOException
     */
    void parse(@NonNull BTInputStream input) throws StreamParseException, IOException;
}