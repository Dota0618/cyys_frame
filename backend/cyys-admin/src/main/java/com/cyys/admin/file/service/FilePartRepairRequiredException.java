package com.cyys.admin.file.service;

import com.cyys.common.web.ApiException;

/** 只提交损坏分片的待补传状态，不提交成功文件；不能被一般事务失败规则吞掉。 */
final class FilePartRepairRequiredException extends ApiException {
    FilePartRepairRequiredException() { super(422, "分片内容损坏或缺失，请选择原文件继续上传"); }
}
