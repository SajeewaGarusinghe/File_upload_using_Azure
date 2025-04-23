package org.iit.demo1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
@Data
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
    private Integer id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "scan_status")
    private String scanStatus;

    @Column(name = "scan_date")
    private LocalDateTime scanDate;

    @Column(name = "clean_file_url")
    private String cleanFileUrl;


}
