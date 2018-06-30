package fun.lib.actor.core;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

public final class DFHttpMultiData {

	private final HttpPostRequestDecoder reqDecoder;
	private final List<DFHttpData> lsData;
	private final int partNum;
	
	protected DFHttpMultiData(HttpPostRequestDecoder reqDecoder) {
		this.reqDecoder = reqDecoder;
		List<InterfaceHttpData> lsReqData = reqDecoder.getBodyHttpDatas();
		if(lsReqData.isEmpty()){
			lsData = null;
			partNum = 0;
		}else{
			int tmpNum = 0;
			lsData = new ArrayList<>(lsReqData.size());
			for(InterfaceHttpData reqData : lsReqData){
				if(reqData.getHttpDataType() == HttpDataType.FileUpload){
					FileUpload fUp = (FileUpload) reqData;
					String tmpFile = fUp.getFilename();
					if(tmpFile == null || tmpFile.equals("")){
						continue;
					}
					DFHttpData data = new DFHttpData(fUp);
					lsData.add(data);
					++tmpNum;
				}
			}
			partNum = tmpNum;
		}
	}
	protected void destroy(){
		reqDecoder.cleanFiles();
		reqDecoder.destroy();
	}
	
	//api
	public int getPartNum(){
		return partNum;
	}
	public DFHttpData getPart(int idx){
		if(idx>-1 && idx<partNum){
			return lsData.get(idx);
		}
		return null;
	}
	
	
}
