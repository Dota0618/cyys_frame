package com.cyys.admin.file.service;

import java.io.IOException;

final class CorruptFilePartException extends IOException {
    final int index;
    CorruptFilePartException(int index) { super("Stored part verification failed"); this.index = index; }
}
