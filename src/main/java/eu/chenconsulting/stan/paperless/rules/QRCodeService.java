package eu.chenconsulting.stan.paperless.rules;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.apache.commons.io.FileUtils;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.content.rules.ChangedPropertyFact;
import org.jahia.services.content.rules.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import org.apache.commons.io.FilenameUtils;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;

import org.kie.api.runtime.ObjectFilter;
import org.drools.core.spi.KnowledgeHelper;

import java.nio.file.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.jahia.settings.SettingsBean;

import javax.jcr.RepositoryException;

public class QRCodeService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private static final String DEFAULT_SERVER_URL_NAME = "localhost";
    private static final String DEFAULT_SERVER_URL_SCHEME = "http://";
    private static final String DEFAULT_SERVER_URL_PORT = "80";

    private QRCodeService qrCodeService;

    public void setQrCodeService(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    private static File contentTempFolder;

    private QRCodeService() {
    }

    // Initialization on demand holder idiom: thread-safe singleton initialization
    private static class Holder {
        static final QRCodeService INSTANCE = new QRCodeService();

        static {
            contentTempFolder = new File(SettingsBean.getInstance().getTmpContentDiskPath());
            if (!contentTempFolder.exists()) {
                contentTempFolder.mkdirs();
            }
        }
    }

    public static QRCodeService getInstance() {
        return Holder.INSTANCE;
    }


    public void createCode(AddedNodeFact fileNode, KnowledgeHelper drools) throws Exception {
        String name = "qrcode";
        long timer = System.currentTimeMillis();
        if (fileNode.getNode().hasNode(name)) {
            JCRNodeWrapper node = fileNode.getNode().getNode(name);
            Calendar codeDate = node.getProperty("jcr:lastModified").getDate();
            Calendar contentDate = fileNode.getNode().getNode("jcr:content").getProperty("jcr:lastModified").getDate();
            if (contentDate.after(codeDate)) {
                AddedNodeFact qrNode = new AddedNodeFact(node);
                File f = getQRFile(fileNode, drools);
                if (f == null) {
                    return;
                }
                drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_DATA, f, drools));
                drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_LASTMODIFIED, new GregorianCalendar(), drools));
            }
        } else {
            File f = getQRFile(fileNode, drools);
            if (f == null) {
                return;
            }

            AddedNodeFact qrNode = new AddedNodeFact(fileNode, name, "jnt:resource", drools);
            if (qrNode.getNode() != null) {
                drools.insert(qrNode);
                drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_DATA, f, drools));
                //drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_MIMETYPE, fileNode.getMimeType(), drools));
                drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_MIMETYPE, "image/jpeg", drools));
                drools.insert(new ChangedPropertyFact(qrNode, Constants.JCR_LASTMODIFIED, new GregorianCalendar(), drools));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("qrCode for node {} created in {} ms", new Object[]{fileNode.getNode().getPath(), System.currentTimeMillis() - timer});
        }
    }

    /*
    //for updates? (problem with package private method...
    public void createCode(ChangedPropertyFact propertyWrapper, KnowledgeHelper drools) throws Exception {
        final JCRPropertyWrapper property = propertyWrapper.getProperty();
        final JCRSessionWrapper session = property.getSession();
        JCRNodeWrapper node = session.getNodeByIdentifier(propertyWrapper.getStringValue());
        createCode(new AddedNodeFact(node), drools);
    }
    */


    protected File getQRFile(AddedNodeFact fileNode, KnowledgeHelper drools) throws Exception {
        String fileExtension = FilenameUtils.getExtension(fileNode.getName());
        // no need to resize the small image for thumbnail
        final File f = File.createTempFile("qrcode", "."+fileExtension, contentTempFolder);
        //JCRContentUtils.downloadFileContent(fileNode.getNode(), f);
        ImageType destImgType;

        //File iw = qrCodeService.getImage(fileNode.getNode());
        switch (fileExtension.toLowerCase()) {
            case "jpg":  destImgType = ImageType.JPG;
                break;
            case "gif":  destImgType = ImageType.GIF;
                break;
            case "png":  destImgType = ImageType.PNG;
                break;
            case "bmp":  destImgType = ImageType.BMP;
                break;
            default: destImgType = ImageType.JPG;
                break;
        }

        String serverAdressUrl = fileNode.getNode().getResolveSite().getServerName();
        String serverBaseAbsUrl = DEFAULT_SERVER_URL_SCHEME+serverAdressUrl+":"+DEFAULT_SERVER_URL_PORT;
        if (serverAdressUrl.equalsIgnoreCase("localhost") || serverAdressUrl.endsWith(".local")) {
            serverBaseAbsUrl = DEFAULT_SERVER_URL_SCHEME+serverAdressUrl+":8080";
        }
        File qrw = QRCode.from(serverBaseAbsUrl+fileNode.getNode().getUrl()).to(destImgType).file();
        Path qrfPath = Paths.get(qrw.getPath());
        InputStream inf = Files.newInputStream(qrfPath);
        FileUtils.copyInputStreamToFile(inf, f);
        f.deleteOnExit();
        return f;
    }

    private File getFileWrapper(final AddedNodeFact fileNode, KnowledgeHelper drools) throws Exception {
        String fileExtension = FilenameUtils.getExtension(fileNode.getName());
        Iterator<?> it = drools.getWorkingMemory().iterateObjects(new ObjectFilter() {
            public boolean accept(Object o) {
                if (o instanceof File) {
                    try {
                        return (((File) o).getPath().equals(fileNode.getPath()));
                    } catch (RepositoryException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                return false;
            }
        });
        if (it.hasNext()) {
            return (File) it.next();
        }
        ImageType destImgType;

        //File iw = qrCodeService.getImage(fileNode.getNode());
        switch (fileExtension.toLowerCase()) {
            case "jpg":  destImgType = ImageType.JPG;
                break;
            case "gif":  destImgType = ImageType.GIF;
                break;
            case "png":  destImgType = ImageType.PNG;
                break;
            case "bmp":  destImgType = ImageType.BMP;
                break;
            default: destImgType = ImageType.JPG;
                break;
        }

        File qrw = QRCode.from(fileNode.getNode().getPath()).to(destImgType).file();
        if (qrw == null) {
            return null;
        }
        drools.insertLogical(qrw);
        return qrw;
    }


    public void disposeQRCodeForNode(final AddedNodeFact fileNode, KnowledgeHelper drools) throws Exception {
        Iterator<?> it = drools.getWorkingMemory().iterateObjects(new ObjectFilter() {
            public boolean accept(Object o) {
                if (o instanceof File) {
                    try {
                        return (((File) o).getPath().equals(fileNode.getPath()));
                    } catch (RepositoryException e) {
                        logger.error(e.getMessage(),e);
                    }
                }
                return false;
            }
        });
        for (; it.hasNext(); ) {
            File f = (File) it.next();
            drools.retract(f);
            f.delete();
            f = null;
        }
    }


}





