package com.example.paspaysweets.service;


import kz.paspay.paspayfileworker.grpc.FileworkerProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class FileworkerSendingService {
    private final FileworkerProtoService fileworkerProtoService;

    @Value("${aws.s3.bucket}")
    private String reportBucketName;

    public FileworkerProto.Response sendFileworkerRequest(byte[] result, String fileName) {

        return fileworkerProtoService.getFileworkerServiceResponse(fileName, reportBucketName, "requestId", result);
    }
}
