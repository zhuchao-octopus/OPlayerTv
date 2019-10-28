
/******************************************************************************************************************
    调用 LINK_IF 模块 的统一函数接口
    用户应用层只能调用这个模块的函数
*******************************************************************************************************************/
#include "stdafx.h"
#include <afx.h>
#include "DFCmdTool.h"
#include "DFCmdToolDlg.h"
#include "Com.h"
#include "LKAI.h"

unsigned char g_lkai_comb_mode;                             //0-> not comb mode; 1-> comb mode

unsigned char g_lkai_comm_txpak[CM_LKAI_TXPAK_MAXLEN];      //TX to VCI package

unsigned char g_lkai_comm_rxpak[CM_LKAI_RXPAK_MAXLEN];      //RX to VCI package


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#define CM_LKAI_PARA_MAXLEN             200 //LKAI 向LKIF传递参数的最大长度, 将设置参数打包成 BYTE *para 的长度
                                            //现有参数打包转换后远达不到这个长度



/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/********************************************************************************************************************
描述:   对 VCI 公共 发送 接收 缓冲区 进行重置
        如果当前 公共缓冲区 处于 组合打包模式， 则不进行重置
********************************************************************************************************************/
void vci_reset_txrx_buff(void)
{
    if(0 != g_lkai_comb_mode)
    {
        return;
    }
    
    //单命令模式 才对公共缓冲区重置
    unsigned char  pak_init[10] = {0x55,0x00, 0x06,0x00, 0x00, 0x00,0x00, 0x00,0x00, 0xcc};

    memcpy(g_lkai_comm_rxpak, pak_init, sizeof(pak_init));
    memcpy(g_lkai_comm_txpak, pak_init, sizeof(pak_init));

}

void vci_reset_rxbuf(void)
{
    unsigned char  pak_init[10] = {0x55,0x00, 0x06,0x00, 0x00, 0x00,0x00, 0x00,0x00, 0xcc};

    memcpy(g_lkai_comm_rxpak, pak_init, sizeof(pak_init));

}
/********************************************************************************************************************
描述:   获取命令块的 总长度
********************************************************************************************************************/
char txcmd_read_cmdlen(unsigned char  *pcmd_buf, unsigned short  *cmdlen)
{
    *cmdlen = *( (unsigned short *)(pcmd_buf+CM_POS_TXCMD_LEN) );

    return 1;
}

/********************************************************************************************************************
描述:   获取命令块的 执行延时间隔
********************************************************************************************************************/
char txcmd_read_intm(unsigned char  *pcmd_buf, unsigned short  *intm)
{
    *intm = *( (unsigned short *)(pcmd_buf+CM_POS_TXCMD_INTM) );

    return 1;
}

/********************************************************************************************************************
描述:   获取命令块的 命令字
********************************************************************************************************************/
char txcmd_read_cmdid(unsigned char  *pcmd_buf, unsigned char  *cmdid)
{
    *cmdid = *( (unsigned char *)(pcmd_buf+CM_POS_TXCMD_ID) );

    return 1;
}

/********************************************************************************************************************
描述:   获取 命令块个数
********************************************************************************************************************/
char txpak_read_tcmd(unsigned char  *ptxpak, unsigned char  *tcmd)
{
    *tcmd = *((unsigned char  *)(ptxpak+CM_POS_TXPAK_TCMD));
    return 1;
}

/********************************************************************************************************************
描述:   获取 数据包 总的有效字节数
********************************************************************************************************************/
char txpak_read_tlen(unsigned char  *ptxpak, unsigned short  *tlen)
{
    *tlen = *((unsigned short  *)(ptxpak+CM_POS_TXPAK_TLEN));

    return 1;
}

/********************************************************************************************************************
描述:   获取 数据包 总的有效字节数
********************************************************************************************************************/
char txpak_write_tlen(unsigned char  *ptxpak, unsigned short  tlen)
{
    *((unsigned short  *)(ptxpak+CM_POS_TXPAK_TLEN)) = tlen;

    return 1;
}

/********************************************************************************************************************
描述:   读取 接收数据包 中指定命令块 的起始位置
********************************************************************************************************************/
char txpak_read_cmdpos(unsigned char  *ptxpak, unsigned char  cmdnum, unsigned short  *cmdpos)
{
    unsigned char   tcmd;
    unsigned short  tmp_pos, cmd_len;
    int i;
    
    txpak_read_tcmd(ptxpak, &tcmd);
	
	if(0 == cmdnum)
	{
		*cmdpos = CM_POS_TXPAK_SBLK;
		return 1;
	}

    if(cmdnum >= tcmd)
    {
        *cmdpos = CM_POS_TXPAK_SBLK;
        return 0;
    }
    
    //加上固定格式头 5 字节
    tmp_pos = CM_POS_RXPAK_SBLK;
    for(i=0; i<cmdnum; i++)
    {
        txcmd_read_cmdlen(ptxpak+tmp_pos, &cmd_len);
        tmp_pos += cmd_len;
    }
    
    *cmdpos = tmp_pos;
    
    return 1;

}

/********************************************************************************************************************
描述:   向 缓冲区 添加一个命令块
********************************************************************************************************************/
char txpak_append_cmd(unsigned char *ptxpak, unsigned char *pcmdblk)
{
    unsigned char  tcmd;
    unsigned short cmdpos;
    unsigned short cmdlen;
    unsigned short tlen;
    
    txpak_read_tcmd(ptxpak, &tcmd);
    if(0 == tcmd)
    {
        txpak_read_cmdpos(ptxpak, 0, &cmdpos);
    }
    else
    {
        txpak_read_cmdpos(ptxpak, tcmd-1, &cmdpos);
        txcmd_read_cmdlen(ptxpak+cmdpos, &cmdlen);
        cmdpos += cmdlen;
    }
    
    txcmd_read_cmdlen(pcmdblk, &cmdlen);

    if( (cmdpos+cmdlen) > CM_LKAI_TXPAK_MAXLEN )
    {
        return 0;
    }

    memcpy(ptxpak+cmdpos, pcmdblk, cmdlen);
    
    txpak_read_tlen(ptxpak, &tlen);
    
    
    txpak_write_tlen(ptxpak, tlen+cmdlen);
    
    *(ptxpak+CM_POS_TXPAK_TCMD) = (tcmd+1);
    
    return 1;
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/********************************************************************************************************************
描述:   将 长度 读出 从 回传命令块 缓冲区
********************************************************************************************************************/
char rxcmd_read_cmdlen(unsigned char  *prxcmd, unsigned short  *cmdlen)
{
    *cmdlen = *( (unsigned short *)(prxcmd+CM_POS_RXCMD_LEN) );

    return 1;
}

/********************************************************************************************************************
描述:   将 命令字 字节 读出 从 回传命令块 缓冲区
********************************************************************************************************************/
char rxcmd_read_cmdid(unsigned char  *prxcmd, unsigned char  *cmdid)
{
    *cmdid = *(prxcmd+CM_POS_RXCMD_ID);

    return 1;
}

/********************************************************************************************************************
描述:   将 命令字 字节 读出 从 回传命令块 缓冲区
********************************************************************************************************************/
char rxcmd_read_cmdres(unsigned char  *prxcmd, unsigned char  *cmdres)
{
    *cmdres = *(prxcmd+CM_POS_RXCMD_RES);

    return 1;
}

/********************************************************************************************************************
描述:   获取 包序号
********************************************************************************************************************/
char rxpak_read_pakidx(unsigned char  *prxpak, unsigned char  *pakidx)
{
    *pakidx = *(prxpak+CM_POS_RXPAK_PAKIDX);
    return 1;
}

/********************************************************************************************************************
描述:   获取 整包数据字节数
********************************************************************************************************************/
char rxpak_read_tlen(unsigned char  *prxpak, unsigned short  *tlen)
{
    *tlen = *((unsigned short  *)(prxpak+CM_POS_RXPAK_TLEN));
    return 1;
}

/********************************************************************************************************************
描述:   获取 命令块个数
********************************************************************************************************************/
char rxpak_read_tcmd(unsigned char  *prxpak, unsigned char  *tcmd)
{
    *tcmd = *((unsigned char  *)(prxpak+CM_POS_RXPAK_TCMD));
    return 1;
}


/********************************************************************************************************************
描述:   获取 指定命令块 的起始位置
********************************************************************************************************************/
char rxpak_read_cmdpos(unsigned char  *prxpak, unsigned char  cmdnum, unsigned short  *cmdpos)
{
    unsigned char   tcmd;
    unsigned short  tmp_pos, cmdlen;
    int i;
    
    rxpak_read_tcmd(prxpak, &tcmd);
    if(cmdnum >= tcmd)
    {
        *cmdpos = CM_POS_RXPAK_SBLK;
        return 0;
    }
    
    //加上固定格式头 5 字节
    tmp_pos = CM_POS_RXPAK_SBLK;
    for(i=0; i<cmdnum; i++)
    {
        rxcmd_read_cmdlen(prxpak+tmp_pos, &cmdlen);
        tmp_pos += cmdlen;
    }
    
    *cmdpos = tmp_pos;
    
    return 1;
}

/********************************************************************************************************************
描述:   获取 指定命令块 的长度
********************************************************************************************************************/
char rxpak_read_cmdlen(unsigned char  *prxpak, unsigned char  cmdnum, unsigned short  *cmdlen)
{
    unsigned short  cmdpos;
    
    if(0 == rxpak_read_cmdpos(prxpak, cmdnum, &cmdpos))
    {
        *cmdlen = 0;
        return 0;
    }
    
    rxcmd_read_cmdlen(prxpak+cmdpos, cmdlen);
    
    return 1;
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
unsigned char calc_pakbuf_xor(unsigned char *pbuf)
{
    unsigned short tlen;
	unsigned char  xorsum=0;
    
    txpak_read_tlen(pbuf, &tlen);
    
	int i;
	for(i=0; i<(tlen-1); i++)
	{	//get xor cs byte
		xorsum ^= pbuf[i];
	}
	
	return xorsum;
}

void showConvertByte2Char(BYTE bInput, unsigned char *pChar)
{
	pChar[0] = (bInput>>4) + 0x30;
	if(pChar[0]>0x39)
		pChar[0] += 0x07;
	
	pChar[1] = (bInput&0x0F) + 0x30;
	if(pChar[1]>0x39)
		pChar[1] += 0x07;
	
	pChar[2] = '\0';
	
}

CString showConvertHexBuf2String(BYTE *pBHexBuf, WORD wDSize)
{
	CString strResult;
	BYTE bTempByte;
	unsigned char chBuf[10];
	
	strResult.Empty();
	for(int i=0; i<wDSize; i++)
	{
		bTempByte = *pBHexBuf++;
		showConvertByte2Char(bTempByte,chBuf);
		chBuf[2] = ' ';
		chBuf[3] = '\0';
		CString strT = chBuf;
		strResult += strT;
	}
	
	return strResult;
	
}

void showAddCmdList(BYTE *pCmdTRX)
{
	CString str;

	WORD CmdLen;
	
	CmdLen = *((unsigned short *)(pCmdTRX + CM_POS_TXPAK_TLEN));

	str = showConvertHexBuf2String(pCmdTRX, CmdLen);
	
	g_strCmdList += str;
		
}

/********************************************************************************************************************
描述:   发送格式化数据包， 并等待接收VCI的回复
        多命令块组合还未结束前， 不会执行发送接收
********************************************************************************************************************/
unsigned char vci_send_recv(unsigned char *ptxd, unsigned char *prxd)
{
    unsigned short tlen, real_txlen, real_rxlen, com_rxlen;
    unsigned short com_rxtlen = 0;
    unsigned char  PakIdx;
    unsigned char  res = 0;
    unsigned char  ReadBuf[CM_LKAI_RXPAK_MAXLEN];

    unsigned char xorsum;

	//完善格式
	xorsum = calc_pakbuf_xor(ptxd);
    txpak_read_tlen(ptxd, &tlen);
    ptxd[tlen-1] = xorsum;
    
    
    //当 组合命令 模式结束后才进行 发送接收
	CCom m_ComPort;
	DCB m_PortDcb;
	CString strComName;

//    strComName="COM1"
//    strComName="\\\\.\\COM67";

    strComName = g_strComName;
	if(!m_ComPort.OpenConnect(strComName))
	{
	    res = 0;
		return res;
	}

	m_PortDcb.BaudRate = CBR_115200;
	m_PortDcb.ByteSize = 8;
	m_PortDcb.StopBits = 0;
	m_PortDcb.Parity = 0;

	m_ComPort.SetCommDCB(&m_PortDcb);
	m_ComPort.AutoSetTimeOut();

	//display txcmd to edit box
	g_strCmdList += "\r\n \r\nSend: ";
	showAddCmdList(ptxd);

//	SetDlgItemText(IDC_EDIT_CMDLIST, m_strCmdList);

	//send to vci
	PakIdx = ptxd[CM_POS_TXPAK_PAKIDX];
	txpak_read_tlen(ptxd, &tlen);
	real_txlen = m_ComPort.WriteDataToComm(ptxd, tlen);
	
	if(tlen != real_txlen)
	{
        m_ComPort.CloseComm();
	    res = 0;
		return res;
	}
	
	//wait from vci
	//read header of package
	int i;
  	res = 1;
    while(1)
    {
    	for(i=0; i<4; i++)
    	{
    	    com_rxlen = m_ComPort.readEx(&ReadBuf[i], 1, 5000);
    	    com_rxtlen += com_rxlen;
            if(0 == com_rxlen)
            {
                res = 0;
                break;
            }
        }
    	
    	if(0 == res)
    	{
    	    break;
    	}
    	
    	real_rxlen = *((unsigned short*)(&ReadBuf[CM_POS_RXPAK_TLEN]));
    	
    	com_rxlen = m_ComPort.readEx(&ReadBuf[CM_POS_RXPAK_TCMD], (real_rxlen-4), real_rxlen);
    	com_rxtlen += com_rxlen;
    	
    	if(com_rxlen != (real_rxlen-4))
    	{
    	    res = 0;
    	    break;
    	}
    	
    	break;
    }

    if(1 == res)
    {
    	memcpy(prxd, ReadBuf, real_rxlen);
    }
    
	//display cmd to edit box
	if(0 == res)
	{
		g_strCmdList += "\r\nRecv: Error, ";
	}
	else
	{
		g_strCmdList += "\r\nRecv: ";
	}
    
    *((unsigned short*)(&ReadBuf[CM_POS_RXPAK_TLEN])) = com_rxtlen;
    showAddCmdList(ReadBuf);
//	SetDlgItemText(IDC_EDIT_CMDLIST, m_strCmdList);
    
    m_ComPort.CloseComm();
    
    return res;
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/********************************************************************************************************************
描述:   从接收数据总包中 获取指定命令块 的回复数据
********************************************************************************************************************/
unsigned char rxpak_read_cmdblk_dat(
//                    unsigned char *ptxpak,      //待发送的数据总包指针, 目前固定传入指向 全局变量区
//                    unsigned char *prxpak,      //用于接收的数据总包指针, 目前固定传入指向 全局变量区
                    unsigned char  rxcmdnum,    //获取命令块的序号, 从 0 开始
                    unsigned char *prxdat,      //用于存放接收总包中第 num 个 命令块中数据区 的指针
                    unsigned short rxdat_size   //prxdat区的允许最大的字节数
                    )
{
    unsigned short cmdpos, rxcmdlen;
    unsigned char  txcmdid, rxcmdid;
    unsigned char  tcmd;
    
    unsigned char *ptxpak = g_lkai_comm_txpak;      //待发送的数据总包指针, 目前固定传入指向 全局变量区
    unsigned char *prxpak = g_lkai_comm_rxpak;      //用于接收的数据总包指针, 目前固定传入指向 全局变量区
    
    //返回的命令块个数 不可能大于发送的总命令块个数
    txpak_read_tcmd(ptxpak, &tcmd);
    if(rxcmdnum >= tcmd)
    {
        return 0;
    }
    
    //读取 发送命令块 的 cmdid
    txpak_read_cmdpos(ptxpak, rxcmdnum, &cmdpos);
    txcmd_read_cmdid(ptxpak+cmdpos, &txcmdid);

    //读取 接收命令块 的 cmdid
    rxpak_read_cmdpos(prxpak, rxcmdnum, &cmdpos);
    rxcmd_read_cmdid(prxpak+cmdpos, &rxcmdid);
    rxcmd_read_cmdlen(prxpak+cmdpos, &rxcmdlen);

    if(rxcmdlen > rxdat_size)
    {
        return 0;
    }

    //判断 接收命令块 VCICMD_xxx 是否与发送一致
    if(rxcmdid != txcmdid)
    {
        return 0;
    }
    
    //注意, 返回上层的数据中 只有命令块 中的 cmddat 部分， 不含 cmdlen(2B) 和 cmdid(1B) 总共3字节
    memcpy(prxdat, prxpak+cmdpos+CM_POS_RXCMD_DAT, rxcmdlen-3);

    return 1;
}


/********************************************************************************************************************
描述:   非打包的单一命令块发送接收
********************************************************************************************************************/
unsigned char vci_aces_single_cmdblk_pak(
                    unsigned char *ptxpak,      //待发送的数据总包指针, 目前固定传入指向 全局变量区
                    unsigned char *prxpak,      //用于接收的数据总包指针, 目前固定传入指向 全局变量区
                    unsigned char *prxdat,      //用于存放接收总包中第一个 命令块中数据区 的指针
                    unsigned short rxdat_size   //prxdat区的允许最大的字节数
                    )
{
    unsigned char  res = 0;

	if(0 != g_lkai_comb_mode)
    {   //打包还未结束，不执行实际的发送接收
        return res;
    }
    
    //以下是单命令块的执行
    //发送格式化数据包， 并等待接收VCI的回复
    if(0 == vci_send_recv(ptxpak, prxpak))
    {
        return res;
    }
    
    res = rxpak_read_cmdblk_dat(0, prxdat, rxdat_size);
    
    return res;
}

/********************************************************************************************************************
描述:   参数设置类型的函数 判断执行结果
        这类函数只有执行结果，没有 ECU 命令的交互
********************************************************************************************************************/
unsigned char vci_set_para_single(
                    unsigned char *ptxpak,      //待发送数据总包指针
                    unsigned char *prxdat,      //存放接收第一个命令块中数据区 的指针
                    unsigned short rxdat_size   //prxdat 的最大范围
                    )
{
    unsigned char  cmdres = 0;

    // 判断 参数设置类 命令块 的执行结果
    if( 0 == vci_aces_single_cmdblk_pak(ptxpak, g_lkai_comm_rxpak, prxdat, rxdat_size) )
    {
        return 0;
    }

    //读取 接收命令块 的 cmdres
    cmdres = prxdat[0];
    if(CM_RXCMD_OK != cmdres)
    {
        return 0;
    }

    return 1;
}

/********************************************************************************************************************
描述:   设置 公共缓冲区 处于 组合打包模式
        同时重置 公共 发送 接收 缓冲区 进行重置
********************************************************************************************************************/
void lkai_comb_start(void)
{
    unsigned char  pak_init[20] = {0x55,0x00, 0x06,0x00, 0x00, 0x00,0x00, 0x00,0x00, 0xcc};

    memcpy(g_lkai_comm_rxpak, pak_init, sizeof(pak_init));
    memcpy(g_lkai_comm_txpak, pak_init, sizeof(pak_init));
    
    g_lkai_comb_mode = 1;
}

/********************************************************************************************************************
描述:   设置 公共缓冲区 结束 组合打包模式
        同时 调用 上下位机的通讯， 执行 之前已打包好的命令数据 
********************************************************************************************************************/
unsigned char lkai_comb_endpro(void)
{
    unsigned char res = 0;
    
    if(0 == g_lkai_comb_mode)
    {    
        return res;
    }
    
    g_lkai_comb_mode = 0;
    
    res = vci_send_recv(g_lkai_comm_txpak, g_lkai_comm_rxpak);
    
    return res;
}


/********************************************************************************************************************
    calculate total real data bytes in txrx
    计算格式化数据中 整个数据的总字节长度
********************************************************************************************************************/
unsigned short link_read_txrx_datlen(BYTE *ptxrx)
{
    unsigned short total_frms;
    unsigned short frm_len;
    unsigned short pos;
    int i;

    if(NULL==ptxrx)
        return 0 ;

    total_frms = *((unsigned short *)ptxrx);

    for(pos=2,i=0; i<total_frms; i++)
    {
        frm_len = *((unsigned short *)(ptxrx+pos));
        pos += frm_len;
    }

    return (pos);
}


/********************************************************************************************************************
    link_if 统一发送接收函数
********************************************************************************************************************/
unsigned short  lkai_send_recv(
                    unsigned short intm,        //执行前的延时
                    BYTE *ptxd,                 //待发送数据, tfrms(2B) + [len(2B)+dat(xB)] + [len(2B)+dat(xB)] ....
                    BYTE *prxd,                 //接收到数据, tfrms(2B) + [len(2B)+dat(xB)] + [len(2B)+dat(xB)] ....
                    unsigned short  rxd_size    //接收缓冲区 recv 的最大长度
                    )
{
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen;   //tlen(2B) + intm(2B) + cmdid(1B) + send(xB) + xor(1B)
    unsigned short datlen;   //
    unsigned short recv_frms;

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();
    
    datlen = link_read_txrx_datlen(ptxd);
    cmdlen = datlen + 6;    //tlen(2B) + intm(2B) + cmdid(1B) + send(xB) + xor(1B)

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_DIAG_SEND_RECV;
    pb += 1;
    memcpy(pb, (BYTE*)(ptxd), datlen);

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    if(0 == vci_aces_single_cmdblk_pak(g_lkai_comm_txpak, g_lkai_comm_rxpak, prxd, rxd_size) )
    {
        memset(prxd, 0, 10);
        return 0;
    }
    
    recv_frms =  *((unsigned short *)prxd);
    *((unsigned short *)prxd) = (recv_frms & 0x0FFF);

    return recv_frms;
}


/********************************************************************************************************************
    设置诊断通讯模块 协议大类
    只是设置 协议类型这个变量
********************************************************************************************************************/
unsigned char lkai_para_ptype_set(
                    unsigned short intm,        //执行前的延时
                    unsigned char comm_type     //逻辑协议类型 t_esLinkType
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 6;   //tlen(2B) + intm(2B) + cmdid(1B) + comm_type(1B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_PRTL_TYPE;
    pb += 1;
    *pb = comm_type;
    
    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);
    
    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}

/********************************************************************************************************************
    设置物理层电气参数
    进行该函数设置之前, 必须已经初始化所用的协议的驱动器, 即所用的 K, CAN, J1850 的驱动已经初始化过
********************************************************************************************************************/
unsigned char lkai_para_phy(
                    unsigned short intm,            //执行前的延时
                    BYTE comm_ipin,                 //接收脚位
                    BYTE comm_opin,                 //发送脚位
                    BYTE comm_is_5v,                //是否使用 5V 通讯电平; K
                    BYTE comm_is_nlogic,            //是否使用 负逻辑 通讯电平; K
                    BYTE comm_klatch_mode,          //是否使能 L线; K
                    BYTE comm_dts                   //Hnibble 输出电平, Lnibble IO选线
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 11;   //tlen(2B) + intm(2B) + cmdid(1B) + para(6B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_PRTL_PHY;
    pb += 1;
    *pb = comm_ipin;
    pb += 1;
    *pb = comm_opin;
    pb += 1;
    *pb = comm_is_5v;
    pb += 1;
    *pb = comm_is_nlogic;
    pb += 1;
    *pb = comm_klatch_mode;
    pb += 1;
    *pb = comm_dts;
    
    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);
    
    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}


/********************************************************************************************************************
描述:   否定应当的默认处理
********************************************************************************************************************/
unsigned char lkai_para_nrc_default(
                    unsigned short intm,              //执行前的延时
                    uint32_t nrc_recv_tmxms,          //默认处理 收到否定应答后 等待响应的最大时间
                    WORD  nrc_txrx_tmcnt,             //默认处理 否定应答重复等待次数
                    WORD  nrc_code_offset,            //否定码 nrc 在7F之后的位置
                    BYTE  nrc_type                    //否应答 类型 如7F / BX
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 16;   //tlen(2B) + intm(2B) + cmdid(1B) + para((4+2+2+1)B)

    //先转换为 xbt
    nrc_recv_tmxms = nrc_recv_tmxms*CM_TIME_LINK_PBASE;

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_NRC_DEFAULT;
    pb += 1;
    memcpy(pb, (BYTE*)(&nrc_recv_tmxms), sizeof(uint32_t));
    pb += 4;
    memcpy(pb, (BYTE*)(&nrc_txrx_tmcnt), sizeof(WORD));
    pb += 2;
    memcpy(pb, (BYTE*)(&nrc_code_offset), sizeof(WORD));
    pb += 2;
    memcpy(pb, (BYTE*)(&nrc_type), sizeof(BYTE));

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;
 
}

/********************************************************************************************************************
    设置kuart参数
********************************************************************************************************************/
unsigned char lkai_para_kuart( 
                    unsigned short intm,        //执行前的延时
                    uint32_t kuart_bps,         //kuart 通讯速率
                    BYTE  kuart_bits,           //kuart 字节位数
                    BYTE  kuart_stopb,          //kuart 停止位
                    BYTE  kuart_parity          //kuart 校验位
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 12;   //tlen(2B) + intm(2B) + cmdid(1B) + para((4+1+1+1)B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_KUART_BIT_FMT;
    pb += 1;
    memcpy(pb, (BYTE*)(&kuart_bps), sizeof(uint32_t));
    pb += 4;
    memcpy(pb, (BYTE*)(&kuart_bits), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&kuart_stopb), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&kuart_parity), sizeof(BYTE));

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;
 

}

/********************************************************************************************************************
    设置接收最大帧数
********************************************************************************************************************/
unsigned char lkai_para_recv_maxfrms(
                    unsigned short intm,        //执行前的延时
                    WORD comm_recv_max_frm     //最大接收帧数
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 7;   //tlen(2B) + intm(2B) + cmdid(1B) + para((2)B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_RECV_MAX_FRMS;
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_recv_max_frm), sizeof(unsigned short));

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}

/********************************************************************************************************************
    设置 J1850 协议专有的参数
********************************************************************************************************************/
unsigned char lkai_para_j1850( 
                    unsigned short intm,        //执行前的延时
                    BYTE  vpwm_frm_inner_ack,   //J1850 帧间响应字节
                    BYTE  vpwm_fmt_filt_byte1,  //J1850 帧头过滤字节
                    BYTE  vpwm_fmt_filt_byte2   //J1850 帧头过滤字节
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 8;   //tlen(2B) + intm(2B) + cmdid(1B) + para((3)B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_J1850_SPEC;
    pb += 1;
    memcpy(pb, (BYTE*)(&vpwm_frm_inner_ack), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&vpwm_fmt_filt_byte1), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&vpwm_fmt_filt_byte2), sizeof(BYTE));

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}

/********************************************************************************************************************
    初始化 CAN 控制器 及其输出引脚
    将 CAN 控制器的初始化 和 OBD16 硬件IO选择 一起执行
********************************************************************************************************************/
unsigned char lkai_can_wio_init(
                    unsigned short intm,            //执行前的延时
                    t_ecan_phy_type can_phy_type,   // CAN 控制器类型
                    t_ecanbps can_bps_type,         // 波特率
                    BYTE comm_ipin,                 // 接收脚位
                    BYTE comm_opin,                 // 发送脚位
                    BYTE comm_is_5v,                // 是否使用 5V 通讯电平; K
                    BYTE comm_is_nlogic,            // 是否使用 负逻辑 通讯电平; K
                    BYTE comm_klatch_mode,          // 是否使能 L线; K
                    BYTE comm_dts                   // Hnibble 输出电平, Lnibble IO选线
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 13;   //tlen(2B) + intm(2B) + cmdid(1B) + para(8B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_CAN_INIT;
    pb += 1;
    memcpy(pb, (BYTE*)(&can_phy_type), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&can_bps_type), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_ipin), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_opin), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_is_5v), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_is_nlogic), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_klatch_mode), sizeof(BYTE));
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_dts), sizeof(BYTE));
    
    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);
    
    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;
}


/********************************************************************************************************************
    设置 CAN 掩码ID 和 过滤ID 混合列表
    u32 can_mask_filt[] =
    {
        0x00010002, //1组 掩码ID + 2组 过滤ID
        CANTYP_EXT, 0xFFFFFFE0,  0x18DAF100     //掩码ID -> 低5bit任意值接收, 其余bit必须和0x18DAF100对应的bit相同 
        CANTYP_EXT, 0x18DAF200                  //指定ID
        CANTYP_EXT, 0x18DAF200                  //指定ID
    };
********************************************************************************************************************/
unsigned char lkai_para_can_mask_filt(
                    unsigned short intm,        //执行前的延时
                    uint32_t *pmask_filt        //can_mask_filt[]
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 5;   //tlen(2B) + intm(2B) + cmdid(1B) + para((x)B)
    uint32_t mask_num, filt_num, list_len;
    
    mask_num = *pmask_filt;
    mask_num = ( (mask_num>>16)&0xffff);
    filt_num = *pmask_filt;
    filt_num = (filt_num&0xffff);

    list_len = 4 + filt_num*8 + filt_num*12;
    cmdlen  += list_len;

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_CAN_FITL_MASK_ID;
    pb += 1;
    memcpy(pb, (BYTE*)(pmask_filt), list_len);

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}


/********************************************************************************************************************
    设置 CAN 长帧流控命令
        BYTE  can_mulfrm_req_msg[CM_SIZE_LINKDATA];   //多帧流控制请求命令
              len   type     id           msg_data
        ig: 0f 00   08   e0 07 00 00    30 00 05 00 00 00 00 00
********************************************************************************************************************/
unsigned char lkai_para_can_flow(
                    unsigned short intm,        //执行前的延时
                    unsigned char *pflow        //can_mulfrm_req_msg[]
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 5;   //tlen(2B) + intm(2B) + cmdid(1B) + para((x)B)
    unsigned short flow_len;

    flow_len = *((unsigned short*)(pflow));
    cmdlen += flow_len;
    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_CAN_FLOW;
    pb += 1;
    memcpy(pb, (BYTE*)(pflow), flow_len);

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}

/********************************************************************************************************************
    设置物理层 L - MODE 
********************************************************************************************************************/
unsigned char lkai_para_lline(
                    unsigned short intm,            //执行前的延时
                    unsigned char comm_klatch_mode  //是否使能 L线; K
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 6;   //tlen(2B) + intm(2B) + cmdid(1B) + para((1)B)

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_KLATCH_MODE;
    pb += 1;
    memcpy(pb, (BYTE*)(&comm_klatch_mode), sizeof(unsigned char));

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}



/********************************************************************************************************************
    设置并启动链路保持命令
    目前支持2条链路命令
        pdat -> frms(1B) 低4位命令条数; 最高位是否立即发送: 0-> 设置完成等待一个周期后发送, 1 则立即发送
             -> tmxms(4B)链路命令的间隔
             -> rx_len(2B) K类链路命令要接收 ECU响应的字节数; 其他类型协议 次参数无意义,以接收完整的帧数为回复
             -> [tx_len(2B) + tx_dat((tx_len-2)*B)] 发送的数据帧

  第二条链路 -> rx_len(2B) K类链路命令要接收 ECU响应的字节数; 其他类型协议 次参数无意义,以接收完整的帧数为回复
             -> [tx_len(2B) + tx_dat((tx_len-2)*B)] 发送的数据帧
********************************************************************************************************************/
unsigned char lkai_para_keep_data(
                    unsigned short intm,        //执行前的延时
                    unsigned char *pdat         //
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000]={0};
    unsigned char *pb    = cmdblk;
    unsigned short cmdlen = 5;   //tlen(2B) + intm(2B) + cmdid(1B) + para((x)B)
    unsigned short frms, i;
    unsigned short flen, plen;

    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    frms = 0x0f&(*pdat);

    if( (0==frms) || (frms>2) )
    {   //没有链路数据, 即关闭链路
        *pdat   = 0;    //确保是 0
        cmdlen += 1;    //frms(1B)
    }
    else    //非0则有链路命令
    {
        //先计算出总的数据长度
        plen = 1;  //frms(1B)
        plen += 4;  //时间间隔 tmxms
        for(i=0; i<frms; i++)
        { 
            flen  = *((unsigned short*)(pdat + plen + 2));      // 帧起点位置 + 固定 2B(rx_len)
            flen += 2;   // 补上固定格式2B
            plen += flen;
        }
        
        cmdlen += plen;
        if(cmdlen >= CM_LKAI_PARA_MAXLEN)
        {
            cmdlen = CM_LKAI_PARA_MAXLEN - 5;
        }
    }

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_LINK_KEEP;
    pb += 1;
    memcpy(pb, (BYTE*)pdat, cmdlen);

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;

}

/********************************************************************************************************************
描述:   否定应答的自定义处理方式
********************************************************************************************************************/
unsigned char lkai_para_nrc_spec(
                    unsigned short intm,        //执行前的延时
                    unsigned char total,        //待设定的个数
                    unsigned char *pcode,       //NRC CODE 列表
                    t_esnrc_mode  *pmode,       //NRC MODE 列表
                    uint32_t *ptmxbt_rerx,      //重复接收等等时间 列表
                    uint32_t *ptmcnt_txrx,      //重复接收/发送次数 列表
                    uint32_t *ptmxbt_retx       //重发的时间间隔 列表
                    )
{
	unsigned char  res = 0;
    unsigned char  cmdblk[1000];
    unsigned char  *pb    = cmdblk;
    unsigned short cmdlen = 5;   //tlen(2B) + intm(2B) + cmdid(1B) + para((x)B)
    unsigned short para_len;
	uint32_t tmxbt;
	int i;

    para_len = 1 + total * (1+1+4+4+4);
    cmdlen += para_len;
    
    //重置 公共发送缓冲区 和 接收缓冲区
    vci_reset_txrx_buff();

    //发送命令块 格式化组合
    memcpy(pb, (unsigned char*)(&cmdlen), sizeof(unsigned short));
    pb += 2;
    memcpy(pb, (unsigned char*)(&intm), sizeof(unsigned short));
    pb += 2;
    *pb = VCICMD_NRC_SPEC;
    pb += 1;
    memcpy(pb, (BYTE*)(&total), sizeof(BYTE));
    pb += 1;

    for(i=0; i<total; i++)
    {
        memcpy(pb, (BYTE*)(pcode+i), sizeof(BYTE));
        pb += 1;
        memcpy(pb, (BYTE*)(pmode+i), sizeof(BYTE));
        pb += 1;

        tmxbt = *(ptmxbt_rerx+i);
        tmxbt = tmxbt * CM_TIME_LINK_PBASE;
        memcpy(pb, (BYTE*)(&tmxbt), sizeof(uint32_t));
        pb += 4;

        memcpy(pb, (BYTE*)(ptmcnt_txrx+i), sizeof(uint32_t));
        pb += 4;

        tmxbt = *(ptmxbt_retx+i);
        tmxbt = tmxbt * CM_TIME_LINK_PBASE;
        memcpy(pb, (BYTE*)(&tmxbt), sizeof(uint32_t));
        pb += 4;

    }

    //将组合好的命令块 追加到 格式化的 公共发送缓冲区
    txpak_append_cmd(g_lkai_comm_txpak, cmdblk);

    res = vci_set_para_single(g_lkai_comm_txpak, cmdblk, sizeof(cmdblk));
    
    return res;
}



