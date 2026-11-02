USE [AlacarteRestaurantDB];
GO

-- 1. Kiểm tra và thêm cột order_view_token nếu bảng chưa có
IF NOT EXISTS (
    SELECT 1 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'RestaurantTables' AND COLUMN_NAME = 'order_view_token'
)
BEGIN
    ALTER TABLE [dbo].[RestaurantTables]
    ADD [order_view_token] NVARCHAR(100) NULL;
END
GO

-- 2. Cập nhật dữ liệu token cho tất cả các bàn (từ bàn 1 đến 50)
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '2AD4C3D3-BD00-4463-A5FC-6BD606971219', [qr_token] = 'b3dd8525-7475-453c-bd94-9653bbc9c237' WHERE [table_id] = 1;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'ECD0D83E-B250-4132-8E0E-4F495B99A558', [qr_token] = '40832a16-9164-46fe-a9f9-58ebdbc0db77' WHERE [table_id] = 2;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '5C2DED35-8E02-4902-B9FA-F5ED31252866', [qr_token] = 'ad8ff1c1-e1c6-423d-b4d2-8c88fc8dd26b' WHERE [table_id] = 3;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'D0ED4E87-AE24-40D1-A891-08C0295B4DAF', [qr_token] = 'abcba432-a740-482b-a0ff-acd402773885' WHERE [table_id] = 4;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '82562EB9-782D-4AB7-A2BF-9D49F6B2B26E', [qr_token] = 'bcd2fee8-e679-4e8b-90ba-610c59a00855' WHERE [table_id] = 5;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6B5927D8-9247-4744-9391-157611F78D3E', [qr_token] = '53ab2fd4-215a-4e8d-a442-fcfbbf06a7a0' WHERE [table_id] = 6;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'F5C126A0-1D06-4409-9909-80114C77AC75', [qr_token] = '6b7200db-fde1-48d6-8e85-675e67665fad' WHERE [table_id] = 7;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'B4292215-63F6-4769-9277-D815F0FE264C', [qr_token] = '8157f0b8-f3d8-4bbd-86c3-a70d7ce50264' WHERE [table_id] = 8;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '84C342DC-259A-46D9-B05F-67E25B34F183', [qr_token] = 'c4df0f4a-4857-4558-a238-e789cc046881' WHERE [table_id] = 9;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '77FB46F5-52D9-4265-BAFD-0864396B6D62', [qr_token] = '8be75e96-0b91-4f05-9b5f-73a828eb40aa' WHERE [table_id] = 10;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '3BA8213C-929C-44AC-97C3-0814BAC8EB32', [qr_token] = '77a8ff9b-b06d-48dd-a8ed-a0045d5f0c15' WHERE [table_id] = 11;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '81442A21-A2C4-4787-AD42-13BFAB74705E', [qr_token] = 'cecd78eb-88d5-45bb-a717-614fcf6ab741' WHERE [table_id] = 12;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'AF2529DD-D077-4C81-A793-BA6E965B73C1', [qr_token] = 'c068d100-31df-41d8-b12b-3c705067516f' WHERE [table_id] = 13;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '32F4572C-3BD8-46BD-B58A-73DD31E13260', [qr_token] = '5438568a-dcc9-4562-a515-bacf6ef93f6c' WHERE [table_id] = 14;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '8BAA4074-3701-4BDE-BB04-59649F28FE63', [qr_token] = '0bc1fcb5-4c1f-4ba5-9884-9b96a4a8fe9d' WHERE [table_id] = 15;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '477ABE9E-2570-4501-9CC4-E1D07DF6EDFA', [qr_token] = 'ef20e2f6-88c4-4941-b038-78dc80b5800a' WHERE [table_id] = 16;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '3246E61F-20B0-4BA5-B70E-7AC8C64D5850', [qr_token] = 'f8e2f681-f07e-4147-8479-53f04c43d221' WHERE [table_id] = 17;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '222550F8-1C3C-4CC9-9D0C-6BC43E11027E', [qr_token] = '17cc7e11-e61c-4ef8-97db-0969d92cd422' WHERE [table_id] = 18;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'ADBC6DC0-D787-4EA5-BB2F-924DB3D2E6B2', [qr_token] = '4433e5ec-5d67-470a-a064-0cb34b72eade' WHERE [table_id] = 19;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'A96AA109-5C5E-4214-8FF8-ACABE0FB4B27', [qr_token] = 'ccc494be-fd55-42f0-b2dc-f7c85f1d4fe8' WHERE [table_id] = 20;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'CF7D4065-992E-48E9-8403-75EED5B4F487', [qr_token] = 'b63762c6-981a-45a1-8763-518f4fe158cf' WHERE [table_id] = 21;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'A30918D3-2FFD-4307-AA08-E8C9A1B8322B', [qr_token] = '7bcfa2b7-4b83-41e3-ae40-d801c78d5c3d' WHERE [table_id] = 22;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'E00BCFEF-80B1-453C-9D87-BF6A3FFA0724', [qr_token] = 'ffc72b12-0b01-4f8c-9467-affaafded29b' WHERE [table_id] = 23;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '89DEB0CB-7F2E-4F3E-B07C-CC2D40B362F3', [qr_token] = '2f9234c4-15fa-405c-875a-8e6618391bc6' WHERE [table_id] = 24;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '586902FB-730B-4A5D-831B-B3455012981D', [qr_token] = 'e9595ec4-ce64-4aa4-9a66-49b38d8578d9' WHERE [table_id] = 25;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'C1922056-0649-4B27-9717-E30A9A406687', [qr_token] = 'ead38aa9-52d9-4936-be7a-95e33fb0be4b' WHERE [table_id] = 26;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6EABAF8E-CE09-40FD-B7AF-0681EDC81795', [qr_token] = '414097d6-2ba4-4f2a-a989-f6309922c2da' WHERE [table_id] = 27;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'C059A152-A9F8-43C3-BFF4-20D0A1A6C244', [qr_token] = '92cc337f-4b2b-46f1-8556-2293307390e2' WHERE [table_id] = 28;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '82338405-95F8-4FAE-851A-CCE55FC12DD5', [qr_token] = 'b2109374-ac9a-42b6-91ea-6363fc087b83' WHERE [table_id] = 29;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'B7E891B7-8766-4801-9AA5-EA538D56C0FC', [qr_token] = '4265c829-3b02-4e96-8991-0b7b687ade61' WHERE [table_id] = 30;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '826E19AE-97C8-442F-BA71-8211CA8479CA', [qr_token] = 'eddbd465-0d9f-4523-afea-e3846c991d29' WHERE [table_id] = 31;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'F61F3C3F-983C-45F8-B1BF-EA0678A1CDC7', [qr_token] = '36820bfb-4736-443c-8e7c-5eada99a57ed' WHERE [table_id] = 32;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '718AC5AE-E3F4-476F-B3E2-1D57FE6DE0CC', [qr_token] = '1561033c-c935-4cb1-9e84-c82b60751e8b' WHERE [table_id] = 33;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '2F07A536-2411-4578-9FBE-9B98AF93C137', [qr_token] = '0a9a50bd-e7db-470f-a357-a3f1f66b62b0' WHERE [table_id] = 34;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6BB207B1-D305-4273-A4A1-4B275AFF51EF', [qr_token] = '69279883-d843-4b69-bf59-76790e9a12e1' WHERE [table_id] = 35;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6423F4C0-5CB6-47AF-BC0C-410FA5F558FD', [qr_token] = 'aeee4494-05a4-4bbb-8304-d571d1f39d31' WHERE [table_id] = 36;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '1BECC9B4-EEBF-465B-944A-E5839BB02E1D', [qr_token] = 'f63cd677-0f8f-4d41-970c-b7e022907467' WHERE [table_id] = 37;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '4DB8D01D-EA6D-48C5-B2EB-D52739AA6BC1', [qr_token] = 'e34c211a-23ac-4fd9-938f-07c377a027a5' WHERE [table_id] = 38;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '10BE6F7B-DE04-477C-B22D-8ADDDB62D2A5', [qr_token] = '830772c8-939b-43ab-8139-d1d172fa3763' WHERE [table_id] = 39;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'C2338D15-A58E-46F2-BC93-E824808D35EF', [qr_token] = '08edfa3c-dba4-417f-af78-ff6610bc1376' WHERE [table_id] = 40;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6D4A35BF-B7A4-4379-84D7-ED449D8146EF', [qr_token] = '7556603a-28b4-42ef-b752-55a587ce631e' WHERE [table_id] = 41;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '4D5B8A63-1031-428E-83A6-63B543BFBD42', [qr_token] = '6c43e238-08a0-4519-9670-8e3027c76855' WHERE [table_id] = 42;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'D36EA1B2-A354-4AC4-A0CA-CFCB12997000', [qr_token] = '1e6e1fac-a1a9-4999-966b-3d5e180cae62' WHERE [table_id] = 43;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '6CB2F2CF-6904-4D44-9B7D-236B75536213', [qr_token] = '7891eb62-3397-45e0-ad59-dddad236a1e4' WHERE [table_id] = 44;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '38EBFB22-8F88-4E1F-8D8B-BC35C8DABE27', [qr_token] = '94075728-25ca-461a-91aa-e485bf6ac201' WHERE [table_id] = 45;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '7B67E829-0C31-4DD3-B553-0FAD09420D38', [qr_token] = 'b5c0bac0-e2b6-4524-9e61-a2f1df5832a7' WHERE [table_id] = 46;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'BE5450EF-66DA-4DCF-91E5-D17316ACD4DC', [qr_token] = '796e1a38-53ea-42f7-bb1c-18307ebad502' WHERE [table_id] = 47;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '57B76592-C7FE-4B48-B9C4-41B044FAAE7E', [qr_token] = 'eb71e6fc-d0a9-42e9-84f2-dfe582af37c6' WHERE [table_id] = 48;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = 'FE938842-B55E-4211-A374-BD1C0CDF095B', [qr_token] = '691842cd-4e2c-4929-b418-b52324a2c533' WHERE [table_id] = 49;
UPDATE [dbo].[RestaurantTables] SET [order_view_token] = '2DC0B982-C73C-47FF-A4BE-6E70E7B30F1E', [qr_token] = '50f16958-c1e4-4f57-83e9-9db8f60d1ede' WHERE [table_id] = 50;
GO