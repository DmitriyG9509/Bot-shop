package com.example.paspaysweets.service;

import com.google.protobuf.ByteString;
import kz.paspay.paspayfileworker.grpc.FileworkerProto;
import kz.paspay.paspayfileworker.grpc.FileworkerServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileworkerProtoService {

    @GrpcClient("fileworker-grpc-server")
    private FileworkerServiceGrpc.FileworkerServiceBlockingStub fileworkerServiceBlockingStub;

    public FileworkerProto.Response getFileworkerServiceResponse(String fileName, String bucketName, String requestId,
                                                                 byte[] fileBytes) {
        FileworkerProto.Response response = null;

        try {
            FileworkerProto.SaveFileRequest request = FileworkerProto.SaveFileRequest.newBuilder()
                                                                                     .setFileName(fileName)
                                                                                     .setBucketName(bucketName)
                                                                                     .setRequestId(requestId)
                                                                                     .setFileBytes(ByteString.copyFrom(fileBytes))
                                                                                     .build();
            response = fileworkerServiceBlockingStub.saveFileFromBytes(request);
            log.info("Received response from Fileworker service: {}", response);
        } catch (Exception e) {
            log.error("Error during saveFileFromBytes request: {}", e.getMessage(), e);
        }
        return response;
    }
}
