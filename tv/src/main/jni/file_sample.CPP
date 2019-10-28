#include "L30_DPD.H"
#include "L30_EXP.H"

FATFS g_fs; 
extern t_smake g_smake;                           //vehicle para
//extern t_sunit_para g_sunitp;
#include "G_XCROS.H"

#define FILE_EXT_BASE_ADDR              sFLASH_ADDR_DATA_START
#define DIAG_SUBFILE_MAX_NUM            (20)  
#define TOP_SUBFILE_MAX_NUM             (25)

typedef enum
{
  FT_LIB = 1,	    //输出的库文件
  FT_DTC,	        //故障码子文件
  FT_LDS, 		    //数据流子文件
  FT_STR, 		    //文本库子文件
  FT_CMD, 	        //命令库子文件
  FT_XPR, 	        //表达式子文件
  FT_HLP, 	        //帮助文档子文件
  
}FT_Type_t;		    // Ftype定义

typedef __packed struct tagSubFile
{
  char FileName[36];
  uint32_t Addr;
  uint32_t Size;
}SubFile_t; 


typedef __packed struct tagCommonStruct
{
  uint32_t string_id;
  uint32_t paddr;
}CommonStruct_t;

TopFileInfo_t TopFileInfo;


FIL g_fdst;
char g_path[100] = {0};
uint8_t g_ferror = 0;

__IO uint8_t diag_file_decrypt_key = 0x0;

void decrypt(uint8_t *dst, uint8_t *src, uint8_t decrypt_key, uint32_t len)
{
  while(len--)
  {
    if((*src == decrypt_key)||(*src == 0x0))
    {
      *dst = *src;
    }
    else
    {
      *dst = *src^decrypt_key;
    }
    
    src++;
    dst++;  
  }
}



/*******************************************************************************
* Function Name  : get_diag_file_info_from_sdcard.
* Description    : 获取SD卡中指定地址的诊断文件信息.
* Input          : pvdb，已知的诊断文件名.
* Output         : DiagFileInfo_t *DiagFileInfo
* Return         : 0 成功，非零失败.
*******************************************************************************/
uint8_t get_diag_file_info_from_sdcard(DiagFileInfo_t *DiagFileInfo, char *pvdb)
{
  uint32_t i = 0;
  uint16_t CalcCrc = 0x0, ReadCrC = 0x0;
  uint64_t OffSet = 0x0;
  uint8_t ReadBuf[100] = {0};
  
  //  FIL fdst;
  char path[100] = {0};
  FRESULT res;         /* FatFs function common result code */
  UINT br, cnt;         /* File read/write count */
  
  strcat(path, (const char *)g_smake.make_path);
  strcat(path, "/");
  strcat(path, pvdb);
  
  OffSet = 0x0;
  
  if((strncmp(g_path, path, strlen(path)) != 0)||(g_ferror == 1))
  {
    if(strlen(g_path)!=0)
    {
      g_ferror = 1;
      f_close(&g_fdst);
    }
    memset(g_path, 0x0, sizeof(g_path));
    memcpy(g_path, path, strlen(path));
    if(f_open(&g_fdst,(const char *)path, FA_READ) == FR_OK)
    {   
      g_ferror = 0;
    }
  }
  
  res = f_lseek(&g_fdst, 0x0);
  if(res != FR_OK)
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 0;
  }
  
  cnt = 96;
  res = f_read(&g_fdst, ReadBuf, cnt, &br);
  if(res ||(cnt != br) )
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 2;
  }
  
  
  memset(DiagFileInfo, 0x0, sizeof(DiagFileInfo_t)); 
  
  OffSet += 96;
  
  for(i = 0; i < 46; i++)
  {
    CalcCrc += ReadBuf[i];
  }
  
  ReadCrC = *(uint16_t *)(ReadBuf+46);
  if(CalcCrc != ReadCrC)
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 3;
  }
  
  DiagFileInfo->FileType = (FT_Type_t)ReadBuf[45];
  
  DiagFileInfo->FileLen = *(uint32_t *)(ReadBuf+48);
  DiagFileInfo->FileHeaderLen = *(uint32_t *)(ReadBuf+64);
  DiagFileInfo->SubFileNum = *(uint32_t *)(ReadBuf+80);
  
  OffSet += 0x60;
  
  memset(ReadBuf, 0x0, sizeof(ReadBuf));
  for(i = 0; i < DiagFileInfo->SubFileNum; i++)
  {
    cnt = 0x30;
    res =  f_read(&g_fdst, ReadBuf, cnt, &br);
    if(res || (cnt != br))
    {
      f_close(&g_fdst);
      g_ferror = 1;
      return 4;
    }
    
    memcpy(DiagFileInfo->SubFileArray+i, ReadBuf, 48);   
    
    OffSet += 0x30;
  }
  
  OffSet += 256;
  
  // f_close(&fdst);
  
  return 0;
  
  
  
}




/*
检查获得的字符串是否符合ID[0x0000000000000000]式样
*/

//uint8_t is_id_pointer_pattern(uint8_t *buf, uint32_t buf_size, uint8_t *addr)
uint8_t is_id_pointer_pattern(uint8_t *buf, uint32_t buf_size, uint64_t *addr)
{
  uint32_t size = strlen("ID[0x0000000000000000]")+1;
  
  uint8_t addr_str[32] = {0}; 
  uint64_t dst_addr = 0;
  
  if((buf_size > size)||(buf_size<7))
  {
    return 0;
  }
  
  
  if(strncmp((char *)buf, "ID[0x", 5) == 0)
  {
    if(strncmp((char *)(buf+(buf_size-2)), "]", 1) == 0)
    {
      memcpy(addr_str, (uint8_t *)(buf+5), buf_size-7);
      
      dst_addr = strtoll((char *)addr_str, 0x0, 16);
      *addr = dst_addr;
      return 1;
    }
    
  }
  
  return 0;
}

static uint32_t read_all_subitem_data_from_sdcard(char * pvdb, uint32_t sub_file_base_addr, char * pcvd, uint64_t id, BYTE subitem, BYTE *buf, uint32_t size)
{
  //  FIL fdst;
  char path[100] = {0};
  FRESULT res;         /* FatFs function common result code */
  UINT br, cnt;         /* File read/write count */
  
  uint32_t OffSet;
  strcat(path, (const char *)g_smake.make_path);
  strcat(path, "/");
  strcat(path, pvdb);
  
  uint32_t ret_addr = 0x0;
  uint32_t sub_file_search_start_addr = 0x0, sub_file_search_end_addr = 0x0, total_item = 0x0;
  
  uint8_t ret_val;
  uint8_t decrypt_key;  
  
  OffSet = 29; 
  
  if((strncmp(g_path, path, strlen(path)) != 0)||(g_ferror == 1))
  {
    if(strlen(g_path)!=0)
    {
      f_close(&g_fdst);
    }
    memset(g_path, 0x0, sizeof(g_path));
    memcpy(g_path, path, strlen(path));
    
    if(f_open(&g_fdst,(const char *)path, FA_READ) == FR_OK)
    {
      if(f_size(&g_fdst) < sub_file_base_addr+OffSet)
      {
        g_ferror = 1;
        f_close(&g_fdst);
        return 0;
      }
      g_ferror = 0;
    }
    
  }
  
  //  OffSet = 29; 
  
  //  if(f_open(&fdst,(const char *)path, FA_READ) == FR_OK)
  //  {
  //    if(f_size(&fdst) < sub_file_base_addr+OffSet)
  //    {
  //      f_close(&fdst);
  //      return 0;
  //    }
  
  res = f_lseek(&g_fdst, sub_file_base_addr+OffSet);
  if(res != FR_OK)
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 0;
  }
  
  cnt = 1;
  res = f_read(&g_fdst, &decrypt_key, cnt, &br);
  if(res ||(cnt != br) )
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 0;
  }
  
  diag_file_decrypt_key = decrypt_key;
  
  sub_file_search_start_addr = sub_file_base_addr + 0x30;
  
  if(strstr(pcvd, "_DTC_") != NULL)
  {
    res = f_lseek(&g_fdst, sub_file_search_start_addr+0x38);
    if(res != FR_OK)
    {
      g_ferror = 1;
      f_close(&g_fdst);
      return 0;
    }
    
    cnt = 4;
    res = f_read(&g_fdst, (uint8_t *)&sub_file_search_end_addr, cnt, &br);
    if(res ||(cnt != br) )
    {
      g_ferror = 1;
      f_close(&g_fdst);
      return 0;
    }
    
    decrypt((uint8_t *)&sub_file_search_end_addr,(uint8_t *)&sub_file_search_end_addr,decrypt_key,cnt);
    sub_file_search_end_addr += sub_file_base_addr;
    total_item = (sub_file_search_end_addr-sub_file_search_start_addr)/12;
    
    ret_addr = binary_search_dtc_addr_from_sdcard(&g_fdst,(DtcStruct_t *)sub_file_search_start_addr, total_item, id, diag_file_decrypt_key);
  }
  else
  {
    SDCardReadDecryptBuff(&g_fdst, (uint8_t *)&sub_file_search_end_addr, sub_file_base_addr+0x34, 4, diag_file_decrypt_key);
    sub_file_search_end_addr += sub_file_base_addr;
    
    total_item = (sub_file_search_end_addr-sub_file_search_start_addr)/8;     
    ret_addr = binary_search_common_addr_from_sdcard(&g_fdst,(CommonStruct_t *)sub_file_search_start_addr, total_item, id, diag_file_decrypt_key);
  }
  
  if(ret_addr == 0)
  {
    g_ferror = 1;
    f_close(&g_fdst);
    return 0;
  }
  else
  {
    ret_addr += sub_file_base_addr;
    
    ret_val = edb_vdb_read_all_item_info_from_sdcard(&g_fdst, ret_addr, subitem, buf, size);
    if(ret_val == 0)
    {
      g_ferror = 0;
      // f_close(&fdst);
      return 1;
    }
    
    g_ferror = 1;
    f_close(&g_fdst); 
    return 0;
  } 
  //}
  //return 0;    
}




/*
return 1 成功
*/
uint32_t get_subitem_all_data_from_sdcard(char *pvdb, char *pcvd, uint64_t id, BYTE subitem, BYTE *buf, uint32_t size)
{
  uint32_t len, res;
  uint32_t sub_file_addr = 0;
  DiagFileInfo_t DiagFileInfo;
  uint16_t i;
  uint8_t id_ret, id_p_ret, id_try_cnt;
  uint64_t id_p_addr;
  uint32_t id_buf_siz = 0;
  
  res = get_diag_file_info_from_sdcard(&DiagFileInfo,pvdb);
  if(res)
  {
    return 0; 
  }
  
  for(i = 0; i< DiagFileInfo.SubFileNum; i++)
  {
    len = strlen(DiagFileInfo.SubFileArray[i].FileName);
    if(len != strlen(pcvd))
    {
      continue;
    }
    
    if(strncmp(DiagFileInfo.SubFileArray[i].FileName, pcvd, len) == 0)
    {
      sub_file_addr = DiagFileInfo.SubFileArray[i].Addr;  
      
      id_try_cnt = 0;
      
       
      while(1)
      {
        memset(buf, 0x0, size);
           
        id_ret = read_all_subitem_data_from_sdcard(pvdb, sub_file_addr, pcvd, id, 0,  buf, size);
        
        if(id_ret)
        {
          id_buf_siz = *(uint16_t *)buf;
          id_p_ret = is_id_pointer_pattern(buf+2, id_buf_siz, &id_p_addr); 
          
          if(id_p_ret)
          {
            
            if(id != id_p_addr)
            {
              id = id_p_addr;
              id_try_cnt++;
              
              /* 尝试几次，还没有找到就退出 */
              if(id_try_cnt >= 3)
              {
                return 1;
              }
              continue;
            }
            else
            {
              id_ret =  read_all_subitem_data_from_sdcard(pvdb, sub_file_addr, pcvd, id, subitem,  buf, size);
              return id_ret;
            }
            
            
          }
          else
          { 

            if(subitem == 0)
            {
              return 1; 
            }
            
            id_ret =  read_all_subitem_data_from_sdcard(pvdb, sub_file_addr, pcvd, id, subitem,  buf, size);
            return id_ret;
          }
        }
        else
        {      
          return 0; 
        }
        
      }
      
    }
  }
  
  return 0;
}



//====================<<<<<<<<<<<<<<<<>++++++++++++++++++
static char sdc_get_subitem_info(FIL *fdst, DWORD addr, uint8_t subitem, uint32_t *subitem_addr, uint32_t *subitem_len)
{
  uint8_t substr_item_num = 0, j = 0;
  uint16_t substr_item_len_table[255] = {0};
  uint32_t offset = 0;
  uint32_t subitem_total_len;
  
  /* 参数非法 */
  if(addr == 0x0)
  {
    return 1; 
  }
  
  SDCardReadDecryptBuff(fdst, (uint8_t *)&substr_item_num, addr, 1, diag_file_decrypt_key);
  
  if(subitem == 0xff)
  {
    /* 参数非法 */
    if(substr_item_num >= 0xff)
    {
      return 1; 
    }
    
    SDCardReadDecryptBuff(fdst, (uint8_t *)substr_item_len_table, addr+1, substr_item_num*2, diag_file_decrypt_key);
    
    subitem_total_len = 1;  
    for(j = 0; j < substr_item_num; j++)
    {
      subitem_total_len += 2;
      subitem_total_len += substr_item_len_table[j];
    }
  }
  else
  {
    /* 参数非法 */
    if(substr_item_num <= subitem)
    {
      return 1; 
    }
    
    SDCardReadDecryptBuff(fdst, (uint8_t *)substr_item_len_table, addr+1, substr_item_num*2, diag_file_decrypt_key);
    
    subitem_total_len = substr_item_len_table[subitem];  
    
    offset += addr+1+substr_item_num*2;
    for(j = 0; j < subitem; j++)
    {
      offset += substr_item_len_table[j];
    }
    *subitem_addr = offset;
    
    *subitem_len = subitem_total_len;
    
  }
  
  return 0;
}

/*******************************************************************************
* Function Name  : binary_search.
* Description    : 查找非dtc文本库，即一般文件的地址.
* Input          : array 要查询的非dtc文本库的首地址.
*		 ：array_len 数组长度
*		 ：search_key 要查询的关键字
*		 ：decrpt_key 解密密钥
* Output         : None
* Return         : 非零，找到的dtc文本库的首地址，0失败.
*******************************************************************************/
int sdc_cmd_binary_search(FIL * fdst, uint8_t * array, uint32_t array_len, uint32_t search_key, uint8_t decrpt_key)
{  
  int32_t low = 0, high = array_len - 1, mid = 0;
  uint8_t read_buf[24] = {0};
  
  uint32_t dtc_lds_id, help_id;
  
  while(low <= high)
  {
    mid = (high-low)/2+low;
    
    SDCardReadDecryptBuff(fdst, read_buf, (uint32_t)(array+mid*11), 12, decrpt_key);
    
    dtc_lds_id  = read_buf[3]<<24;
    dtc_lds_id += read_buf[4]<<16;
    dtc_lds_id += read_buf[5]<<8;
    dtc_lds_id += read_buf[6];
    
    if(dtc_lds_id == search_key)
    {
      help_id  = read_buf[7]<<24;
      help_id += read_buf[8]<<16;
      help_id += read_buf[9]<<8;
      help_id += read_buf[10];
      
      return help_id;
    }
    else if(dtc_lds_id < search_key)
    {
      low = mid + 1;
    }
    else
    {
      high = mid - 1;
    }
  }
  
  return 0;
}


